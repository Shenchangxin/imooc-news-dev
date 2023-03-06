package com.imooc.files.service.impl;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.imooc.files.service.UploaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploaderServiceImpl implements UploaderService {

    @Autowired
    public FastFileStorageClient fastFileStorageClient;



    @Override
    public String uploadFdfs(MultipartFile file,String fileExtName) throws Exception{

        StorePath storePath = fastFileStorageClient.uploadFile(file.getInputStream(),file.getSize(),fileExtName,null);

        return storePath.getFullPath();
    }
}
