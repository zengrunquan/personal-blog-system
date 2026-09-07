package com.blog.media.storage;

import com.blog.media.model.MediaAsset;
import com.blog.media.model.MediaType;
import com.blog.media.model.StoredMedia;

import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Path;

public interface MediaFileStore {

    StoredMedia store(Part part, MediaType type) throws IOException;

    Path resolve(MediaAsset asset) throws IOException;

    boolean deleteIfExists(MediaAsset asset) throws IOException;
}
