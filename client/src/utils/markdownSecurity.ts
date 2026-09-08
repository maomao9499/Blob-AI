import sanitizeHtml from 'sanitize-html';

/** 编辑与详情共享的 HTML 白名单；任务复选框只读。 */
export const sanitizeMarkdown = (html: string): string => sanitizeHtml(html, {
  allowedTags: [...sanitizeHtml.defaults.allowedTags, 'img', 'input', 'del', 's', 'details', 'summary'],
  allowedAttributes: {
    '*': ['class', 'id'], a: ['href', 'title'], img: ['src', 'alt', 'title', 'width', 'height'],
    input: ['type', 'checked', 'disabled'], details: ['open'], code: ['class'], td: ['colspan', 'rowspan'], th: ['colspan', 'rowspan'],
  },
  allowedSchemes: ['http', 'https'],
  allowedSchemesByTag: { img: ['http', 'https', 'blob-app'] },
  allowProtocolRelative: false,
  parseStyleAttributes: false,
  transformTags: {
    input: (_tag, attributes) => ({ tagName: 'input', attribs: { type: 'checkbox', disabled: '', ...(attributes.checked !== undefined ? { checked: '' } : {}) } }),
    img: (_tag, attributes) => {
      const src = attributes.src ?? '';
      // 仅将服务端返回的相对图片路径交给桌面认证代理，Markdown 本身保持可迁移。
      if (typeof window !== 'undefined' && window.blobDesktop && /^\/api\/v1\/media\/images\/[a-zA-Z0-9._-]+$/.test(src)) {
        return { tagName: 'img', attribs: { ...attributes, src: `blob-app://app${src}` } };
      }
      if (/^blob-app:/i.test(src)) { const { src: _src, ...rest } = attributes; return { tagName: 'img', attribs: rest }; }
      return { tagName: 'img', attribs: attributes };
    },
  },
});
