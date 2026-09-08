package com.blog.media.service;

import com.blog.media.dao.MediaDao;
import com.blog.media.dao.MediaDaoImpl;
import com.blog.media.model.MediaType;
import com.blog.util.JdbcTransactionManager;
import com.blog.util.TransactionManager;
import com.blog.util.UploadFileDownloadUtil;

import java.util.Objects;

/** 附件下载名查询服务；只读取元数据，避免文件输出期间持有数据库事务。 */
public class AttachmentDownloadNameService {

    private final MediaDao mediaDao;
    private final TransactionManager transactionManager;

    public AttachmentDownloadNameService() {
        this(new MediaDaoImpl(), new JdbcTransactionManager());
    }

    public AttachmentDownloadNameService(
            MediaDao mediaDao,
            TransactionManager transactionManager
    ) {
        this.mediaDao = Objects.requireNonNull(mediaDao, "mediaDao 不能为空");
        this.transactionManager = Objects.requireNonNull(
                transactionManager,
                "transactionManager 不能为空"
        );
    }

    public String resolveDownloadName(String canonicalUrlFileName) {
        String originalName = transactionManager.inTransaction(connection ->
                mediaDao.findOriginalName(
                        connection,
                        MediaType.ATTACHMENT,
                        canonicalUrlFileName
                ).orElse(null)
        );
        return UploadFileDownloadUtil.sanitizeDownloadName(
                originalName,
                canonicalUrlFileName
        );
    }
}
