export const siteConfig = {
  name: '个人博客',
  title: '写代码，也写生活',
  tagline: '记录技术、阅读与日常思考',
  author: '一名热爱编程的开发者',
  // 动态绑定的图片地址不会被 Vite 自动改写，因此显式拼接部署上下文，避免 WAR 子路径下出现破图。
  defaultAvatar: `${import.meta.env.BASE_URL}images/author-portrait.png`,
  defaultBiography: '喜欢分享技术心得，也记录生活里值得慢慢回看的片段。',
  techStack: ['Vue 3 · TypeScript', 'Servlet · Java 11', 'JDBC · MySQL'],
} as const
