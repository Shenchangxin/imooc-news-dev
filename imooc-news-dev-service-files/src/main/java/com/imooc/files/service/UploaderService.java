package com.imooc.files.service;

import org.springframework.web.multipart.MultipartFile;

public interface UploaderService {

    public String uploadFdfs(MultipartFile file, String fileExtName)throws Exception;
}
