package com.xuecheng.manage_cms.dao;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GridPage {
    @Autowired
    GridFsTemplate gridFsTemplate;
    @Autowired
    GridFSBucket gridFSBucket;

    @Test
    public void test1() throws FileNotFoundException {
        File file = new File("d:/index_banner.ftl");
        FileInputStream fileInputStream = new FileInputStream(file);
        ObjectId hello = gridFsTemplate.store(fileInputStream, "hello");
        System.out.println(hello);
    }

    @Test
    public void test2() throws IOException {

        GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is("605d778b024e50c3084d377d")));
        GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        GridFsResource gridFsResource = new GridFsResource(gridFSFile,gridFSDownloadStream);
        String s = IOUtils.toString(gridFsResource.getInputStream(), "UTF-8");
        System.out.println(s);
    }

    @Test
    public void test3() throws IOException {

       gridFsTemplate.delete(new Query(Criteria.where("_id").is("605d778b024e50c3084d377d")));

    }
}
