<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import {
  Bold,
  Code2,
  FileUp,
  Heading1,
  Heading2,
  ImagePlus,
  Italic,
  Link2,
  List,
  ListOrdered,
  Quote,
  Redo2,
  Undo2,
} from 'lucide-vue-next'
import { uploadApi } from '@/api/blog'
import { AppError } from '@/api/types'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const imageInput = ref<HTMLInputElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const uploadError = ref('')
const uploading = ref(false)

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit,
    Link.configure({ openOnClick: false, HTMLAttributes: { rel: 'noopener noreferrer nofollow' } }),
    Image.configure({ allowBase64: false }),
  ],
  editorProps: {
    attributes: {
      class: 'editor-content',
      'aria-label': '文章正文编辑器',
    },
  },
  onUpdate: ({ editor: current }) => {
    emit('update:modelValue', current.getHTML())
  },
})

watch(
  () => props.modelValue,
  (value) => {
    if (editor.value && editor.value.getHTML() !== value)
      editor.value.commands.setContent(value, false)
  },
)

function setLink(): void {
  if (!editor.value) return
  const previous = editor.value.getAttributes('link').href as string | undefined
  const href = window.prompt('请输入链接地址', previous ?? 'https://')
  if (href === null) return
  if (href.trim() === '') {
    editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }
  editor.value.chain().focus().extendMarkRange('link').setLink({ href: href.trim() }).run()
}

async function uploadImage(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !editor.value) return
  uploading.value = true
  uploadError.value = ''
  try {
    const uploaded = await uploadApi.image(file)
    editor.value.chain().focus().setImage({ src: uploaded.url, alt: uploaded.originalName }).run()
  } catch (error) {
    uploadError.value = error instanceof AppError ? error.message : '图片上传失败'
    console.error(
      `[DEBUG] ${new Date().toISOString()} [RichTextEditor.uploadImage] 上传失败`,
      error,
    )
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function uploadFile(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !editor.value) return
  uploading.value = true
  uploadError.value = ''
  try {
    const uploaded = await uploadApi.file(file)
    const safeName = uploaded.originalName.replace(/[<>&"']/g, '')
    editor.value
      .chain()
      .focus()
      .insertContent(`<p><a href="${uploaded.url}" rel="nofollow">下载附件：${safeName}</a></p>`)
      .run()
  } catch (error) {
    uploadError.value = error instanceof AppError ? error.message : '附件上传失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [RichTextEditor.uploadFile] 上传失败`, error)
  } finally {
    uploading.value = false
    input.value = ''
  }
}

onBeforeUnmount(() => editor.value?.destroy())
</script>

<template>
  <div class="rich-editor">
    <div v-if="editor" class="editor-toolbar" aria-label="编辑工具栏">
      <button
        type="button"
        title="一级标题"
        :class="{ active: editor.isActive('heading', { level: 1 }) }"
        @click="editor.chain().focus().toggleHeading({ level: 1 }).run()"
      >
        <Heading1 :size="18" />
      </button>
      <button
        type="button"
        title="二级标题"
        :class="{ active: editor.isActive('heading', { level: 2 }) }"
        @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
      >
        <Heading2 :size="18" />
      </button>
      <button
        type="button"
        title="粗体"
        :class="{ active: editor.isActive('bold') }"
        @click="editor.chain().focus().toggleBold().run()"
      >
        <Bold :size="18" />
      </button>
      <button
        type="button"
        title="斜体"
        :class="{ active: editor.isActive('italic') }"
        @click="editor.chain().focus().toggleItalic().run()"
      >
        <Italic :size="18" />
      </button>
      <button
        type="button"
        title="无序列表"
        :class="{ active: editor.isActive('bulletList') }"
        @click="editor.chain().focus().toggleBulletList().run()"
      >
        <List :size="18" />
      </button>
      <button
        type="button"
        title="有序列表"
        :class="{ active: editor.isActive('orderedList') }"
        @click="editor.chain().focus().toggleOrderedList().run()"
      >
        <ListOrdered :size="18" />
      </button>
      <button
        type="button"
        title="引用"
        :class="{ active: editor.isActive('blockquote') }"
        @click="editor.chain().focus().toggleBlockquote().run()"
      >
        <Quote :size="18" />
      </button>
      <button
        type="button"
        title="代码块"
        :class="{ active: editor.isActive('codeBlock') }"
        @click="editor.chain().focus().toggleCodeBlock().run()"
      >
        <Code2 :size="18" />
      </button>
      <button
        type="button"
        title="链接"
        :class="{ active: editor.isActive('link') }"
        @click="setLink"
      >
        <Link2 :size="18" />
      </button>
      <button type="button" title="上传图片" :disabled="uploading" @click="imageInput?.click()">
        <ImagePlus :size="18" />
      </button>
      <button type="button" title="上传附件" :disabled="uploading" @click="fileInput?.click()">
        <FileUp :size="18" />
      </button>
      <span class="toolbar-spacer"></span>
      <button
        type="button"
        title="撤销"
        :disabled="!editor.can().undo()"
        @click="editor.chain().focus().undo().run()"
      >
        <Undo2 :size="18" />
      </button>
      <button
        type="button"
        title="重做"
        :disabled="!editor.can().redo()"
        @click="editor.chain().focus().redo().run()"
      >
        <Redo2 :size="18" />
      </button>
    </div>
    <EditorContent :editor="editor" />
    <input
      ref="imageInput"
      class="visually-hidden"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp"
      @change="uploadImage"
    />
    <input ref="fileInput" class="visually-hidden" type="file" @change="uploadFile" />
    <p v-if="uploadError" class="field-error">{{ uploadError }}</p>
  </div>
</template>

<style scoped>
.rich-editor {
  border: 1px solid var(--line);
  background: #fffdfa;
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.2rem;
  padding: 0.5rem;
  border-bottom: 1px solid var(--line);
  background: var(--paper-deep);
}

.editor-toolbar button {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
}

.editor-toolbar button:hover:not(:disabled),
.editor-toolbar button.active {
  color: var(--vermilion);
  border-color: var(--line);
  background: var(--paper-light);
}

.editor-toolbar button:disabled {
  cursor: not-allowed;
  opacity: 0.35;
}

.toolbar-spacer {
  flex: 1;
}

:deep(.editor-content) {
  min-height: 420px;
  padding: 1.25rem 1.4rem;
  outline: 0;
  font-family: var(--serif);
  line-height: 1.9;
}

:deep(.editor-content h1),
:deep(.editor-content h2) {
  line-height: 1.4;
}

:deep(.editor-content blockquote) {
  margin-left: 0;
  padding-left: 1rem;
  border-left: 3px solid var(--vermilion);
  color: var(--ink-soft);
}

:deep(.editor-content pre) {
  overflow-x: auto;
  padding: 1rem;
  color: var(--paper-light);
  background: var(--ink);
}

:deep(.editor-content img) {
  max-height: 520px;
  margin: 1.5rem auto;
  object-fit: contain;
}

.field-error {
  padding: 0 1rem 0.7rem;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}

@media (max-width: 640px) {
  :deep(.editor-content) {
    min-height: 340px;
    padding: 1rem;
  }
}
</style>
