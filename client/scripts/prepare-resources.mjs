import { access, cp, lstat, mkdir, mkdtemp, readdir, readFile, readlink, realpath, rename, rm, stat, writeFile } from 'node:fs/promises';
import { constants, createReadStream } from 'node:fs';
import { createHash } from 'node:crypto';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { dirname, isAbsolute, join, relative, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const execute = promisify(execFile);
const defaultClientDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const contained = (root, target) => { const rel = relative(root, target); return rel !== '..' && !rel.startsWith(`..${process.platform === 'win32' ? '\\' : '/'}`) && !isAbsolute(rel); };

export async function inspectRuntime(javaPath) {
  const { stdout } = await execute('/usr/bin/file', ['-b', javaPath]);
  return stdout.includes('arm64') ? 'arm64' : 'unsupported';
}

export async function createManifest(root) {
  const files = [];
  async function visit(directory) {
    for (const name of (await readdir(directory)).sort()) {
      const file = join(directory, name);
      const info = await lstat(file);
      const path = relative(root, file).split('\\').join('/');
      if (info.isSymbolicLink()) {
        const target = await readlink(file);
        const resolved = await realpath(file);
        if (!contained(root, resolved)) throw new Error(`资源符号链接越界: ${path}`);
        files.push({ path, symlink: target, sha256: createHash('sha256').update(target).digest('hex') });
      } else if (info.isDirectory()) await visit(file);
      else if (info.isFile() && path !== 'manifest.json') {
        const hash = createHash('sha256');
        for await (const chunk of createReadStream(file)) hash.update(chunk);
        files.push({ path, sha256: hash.digest('hex') });
      }
    }
  }
  await visit(root);
  return { version: 1, platform: 'darwin', arch: 'arm64', files };
}

export async function prepareResources({ clientDir = defaultClientDir, runtimeDir = process.env.BLOB_JAVA_RUNTIME_DIR, inspectRuntime: inspect = inspectRuntime } = {}) {
  const client = await realpath(clientDir);
  const workspace = await realpath(join(client, '..'));
  const web = await realpath(join(client, 'dist')).catch(() => { throw new Error('缺少前端 web 构建，请先 npm run build'); });
  if (!contained(workspace, web)) throw new Error('前端构建路径超出工作区');
  await access(join(web, 'index.html')).catch(() => { throw new Error('缺少前端 web/index.html'); });
  const target = await realpath(join(workspace, 'server/target')).catch(() => { throw new Error('缺少后端 JAR，请先构建 server'); });
  if (!contained(workspace, target)) throw new Error('后端 JAR 路径超出工作区');
  const jars = (await readdir(target)).filter(name => name.endsWith('.jar') && !name.endsWith('-sources.jar') && !name.endsWith('-javadoc.jar'));
  if (jars.length !== 1) throw new Error('后端 JAR 必须唯一，请清理过期构建产物');
  const jar = await realpath(join(target, jars[0]));
  if (!contained(workspace, jar)) throw new Error('后端 JAR 路径超出工作区');
  if (!runtimeDir || !isAbsolute(runtimeDir)) throw new Error('请设置 BLOB_JAVA_RUNTIME_DIR 为 Java 21 runtime 的绝对路径');
  const runtime = await realpath(runtimeDir).catch(() => { throw new Error('Java runtime 目录不存在'); });
  let javaHome = join(runtime, 'Contents/Home');
  if (!(await stat(javaHome).catch(() => null))?.isDirectory()) javaHome = runtime;
  const java = join(javaHome, 'bin/java');
  await access(java, constants.X_OK).catch(() => { throw new Error('Java runtime 缺少可执行的 Contents/Home/bin/java 或 bin/java'); });
  if (await inspect(java) !== 'arm64') throw new Error('Java runtime 必须为 macOS arm64');
  const release = await readFile(join(javaHome, 'release'), 'utf8');
  if (!/^JAVA_VERSION="21(?:\.|"|-)/m.test(release)) throw new Error('Java runtime 必须为 Java 21');
  const resources = join(client, 'resources');
  const existing = await lstat(resources).catch(() => null);
  if (existing?.isSymbolicLink()) throw new Error('资源目标目录不能是符号链接');
  const staging = await mkdtemp(join(client, '.blob-resources-'));
  try {
    await cp(web, join(staging, 'web'), { recursive: true, dereference: false, verbatimSymlinks: true });
    await mkdir(join(staging, 'server'));
    await cp(jar, join(staging, 'server/blob-server.jar'));
    await mkdir(join(staging, 'runtime/Contents'), { recursive: true });
    await cp(javaHome, join(staging, 'runtime/Contents/Home'), { recursive: true, dereference: false, verbatimSymlinks: true });
    const manifest = await createManifest(staging);
    await writeFile(join(staging, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
    // This exact generated child is the only replaceable directory.
    if (resources !== join(client, 'resources') || !contained(client, resources)) throw new Error('资源目标路径不安全');
    await rm(resources, { recursive: true, force: true });
    await rename(staging, resources);
    return manifest;
  } finally { await rm(staging, { recursive: true, force: true }); }
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  prepareResources().then(manifest => console.log(`已准备 ${manifest.files.length} 个 macOS arm64 资源文件`)).catch(error => { console.error(error.message); process.exitCode = 1; });
}
