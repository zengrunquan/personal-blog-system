import { describe, expect, it } from 'vitest'
import { unwrap } from './http'
import type { ApiResponse, AppError } from './types'

describe('unwrap', () => {
  it('返回成功响应中的 data', async () => {
    const payload: ApiResponse<{ id: number }> = { success: true, data: { id: 7 } }
    const request = Promise.resolve({ data: payload })
    await expect(unwrap(request)).resolves.toEqual({ id: 7 })
  })

  it('保留后端业务错误码与字段错误', async () => {
    const payload: ApiResponse<never> = {
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: '参数校验失败',
        fieldErrors: { title: '标题不能为空' },
      },
    }
    const request = Promise.resolve({ data: payload })
    await expect(unwrap(request)).rejects.toMatchObject({
      code: 'VALIDATION_ERROR',
      fieldErrors: { title: '标题不能为空' },
    } satisfies Partial<AppError>)
  })
})
