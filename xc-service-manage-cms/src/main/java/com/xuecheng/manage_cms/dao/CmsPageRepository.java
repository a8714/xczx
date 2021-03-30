package com.xuecheng.manage_cms.dao;


import com.xuecheng.framework.domain.cms.CmsPage;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CmsPageRepository extends MongoRepository<CmsPage,String> {
    //根据页面名称查询
    CmsPage findByPageName(String pageName);
    CmsPage findAllByPageNameAndAndPageId(String PageName,String PageId);
    //页面名称、站点Id、页面webpath为唯一索引
    CmsPage findByPageNameAndSiteIdAndPageWebPath(String PageName,String SiteId,String PageWebPath);
}
