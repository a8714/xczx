package com.xuecheng.manage_cms.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsConfig;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mockito.internal.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * @author Administrator
 * @version 1.0
 * @create 2018-09-12 18:32
 **/
@Service
public class PageService {

    @Autowired
    CmsPageRepository cmsPageRepository;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    CmsTemplateRepository cmsTemplateRepository;

    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    GridFSBucket gridFSBucket;


    /**
     * 页面查询方法
     * @param page 页码，从1开始记数
     * @param size 每页记录数
     * @param queryPageRequest 查询条件
     * @return
     */
    public QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest){

//        CmsPage cmsPage = new CmsPage();
//
//
//
//            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
//
//        ExampleMatcher exampleMatcher = ExampleMatcher.matching().
//                withMatcher("pageAliase",ExampleMatcher.GenericPropertyMatchers.contains());
//
//        Example<CmsPage> example = Example.of(cmsPage,exampleMatcher);
//        //分页参数
//        page = page-1;
//        Pageable pageable = new PageRequest(page, size);
//        Page<CmsPage> all = cmsPageRepository.findAll(example,pageable);
//        QueryResult queryResult = new QueryResult();
//        queryResult.setList(all.getContent());//数据列表
//        queryResult.setTotal(all.getTotalElements());//数据总记录数
//        QueryResponseResult queryResponseResult = new QueryResponseResult(CommonCode.SUCCESS,queryResult);
//        return queryResponseResult;
        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains());
//条件值
        CmsPage cmsPage = new CmsPage();
//站点ID
        if(StringUtils.isNotEmpty(queryPageRequest.getSiteId())){
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
//页面别名
        if(StringUtils.isNotEmpty(queryPageRequest.getPageAliase())){
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
        }
//创建条件实例
        Example<CmsPage> example = Example.of(cmsPage, exampleMatcher);
//页码
        page = page-1;
//分页对象
        Pageable pageable = new PageRequest(page, size);
//分页查询
        Page<CmsPage> all = cmsPageRepository.findAll(example,pageable);
        QueryResult<CmsPage> cmsPageQueryResult = new QueryResult<CmsPage>();
        cmsPageQueryResult.setList(all.getContent());
        cmsPageQueryResult.setTotal(all.getTotalElements());
//返回结果
        return new QueryResponseResult(CommonCode.SUCCESS,cmsPageQueryResult);
    }

     public CmsPageResult add(CmsPage cmsPage){
         CmsPage byPageNameAndSiteIdAndPageWebPath = this.cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
         if (byPageNameAndSiteIdAndPageWebPath == null){
             this.cmsPageRepository.save(cmsPage);
             return new CmsPageResult(CommonCode.SUCCESS,cmsPage);
         }else if(byPageNameAndSiteIdAndPageWebPath != null){
             ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
         }
         return new CmsPageResult(CommonCode.FAIL,null);
     }

     public CmsPage findById(String id){
         Optional<CmsPage> byId = this.cmsPageRepository.findById(id);
         if(byId.isPresent()){
             return byId.get();
         }
         return null;
     }

     public CmsPageResult update(String id,CmsPage cmsPage){
         CmsPage one = this.findById(id);
         if(one != null){
             one.setTemplateId(cmsPage.getTemplateId());
//更新所属站点
             one.setSiteId(cmsPage.getSiteId());
//更新页面别名
             one.setPageAliase(cmsPage.getPageAliase());
//更新页面名称
             one.setPageName(cmsPage.getPageName());
//更新访问路径
             one.setPageWebPath(cmsPage.getPageWebPath());
//更新物理路径
             one.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
             one.setDataUrl(cmsPage.getDataUrl());
//执行更新
             CmsPage save = cmsPageRepository.save(one);
             if (save != null) {
//返回成功
                 CmsPageResult cmsPageResult = new CmsPageResult(CommonCode.SUCCESS, save);
                 return cmsPageResult;
             }
         }
         return  new CmsPageResult(CommonCode.FAIL,null);
     }

     public ResponseResult delete(String id){
         Optional<CmsPage> byId = this.cmsPageRepository.findById(id);
         if(byId.isPresent()){
            this.cmsPageRepository.delete(byId.get());
             return new ResponseResult(CommonCode.SUCCESS);

         }
         return new ResponseResult(CommonCode.FAIL);
     }

    public String getPageHtml(String pageId){
//获取页面模型数据
        Map model = this.getModelByPageId(pageId);
        if(model == null){
//获取页面模型数据为空
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
//获取页面模板
        String templateContent = getTemplateByPageId(pageId);
        if(StringUtils.isEmpty(templateContent)){
//页面模板为空
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
//执行静态化
        String html = generateHtml(templateContent, model);
        if(StringUtils.isEmpty(html)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        return html;
    }
    //页面静态化
    public String generateHtml(String template,Map model){
        try {
//生成配置类
            Configuration configuration = new Configuration(Configuration.getVersion());
//模板加载器
            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            stringTemplateLoader.putTemplate("template",template);
//配置模板加载器
            configuration.setTemplateLoader(stringTemplateLoader);
//获取模板
            Template template1 = configuration.getTemplate("template");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template1, model);
            return html;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //获取页面模板
    public String getTemplateByPageId(String pageId){
//查询页面信息
        CmsPage cmsPage = findById(pageId);
        if(cmsPage == null){
//页面不存在
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
//页面模板
        String templateId = cmsPage.getTemplateId();
        if(StringUtils.isEmpty(templateId)){
//页面模板为空
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        Optional<CmsTemplate> optional = cmsTemplateRepository.findById(templateId);
        if(optional.isPresent()){
            CmsTemplate cmsTemplate = optional.get();
//模板文件id
            String templateFileId = cmsTemplate.getTemplateFileId();
//取出模板文件内容
            GridFSFile gridFSFile =
                    gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
//打开下载流对象
            GridFSDownloadStream gridFSDownloadStream =
                    gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
//创建GridFsResource
            GridFsResource gridFsResource = new GridFsResource(gridFSFile,gridFSDownloadStream);
            try {
                String content = IOUtils.toString(gridFsResource.getInputStream(), "utf‐8");
                return content;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
        //获取页面模型数据
    public Map getModelByPageId(String pageId){
        //查询页面信息
        CmsPage cmsPage = this.findById(pageId);
        if(cmsPage == null){
        //页面不存在
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        //取出dataUrl
        String dataUrl = cmsPage.getDataUrl();
        if(StringUtils.isEmpty(dataUrl)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(dataUrl, Map.class);
        Map body = forEntity.getBody();
        return body;
    }

}

