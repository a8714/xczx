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
     * ??????????????????
     * @param page ????????????1????????????
     * @param size ???????????????
     * @param queryPageRequest ????????????
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
//        //????????????
//        page = page-1;
//        Pageable pageable = new PageRequest(page, size);
//        Page<CmsPage> all = cmsPageRepository.findAll(example,pageable);
//        QueryResult queryResult = new QueryResult();
//        queryResult.setList(all.getContent());//????????????
//        queryResult.setTotal(all.getTotalElements());//??????????????????
//        QueryResponseResult queryResponseResult = new QueryResponseResult(CommonCode.SUCCESS,queryResult);
//        return queryResponseResult;
        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains());
//?????????
        CmsPage cmsPage = new CmsPage();
//??????ID
        if(StringUtils.isNotEmpty(queryPageRequest.getSiteId())){
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
//????????????
        if(StringUtils.isNotEmpty(queryPageRequest.getPageAliase())){
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
        }
//??????????????????
        Example<CmsPage> example = Example.of(cmsPage, exampleMatcher);
//??????
        page = page-1;
//????????????
        Pageable pageable = new PageRequest(page, size);
//????????????
        Page<CmsPage> all = cmsPageRepository.findAll(example,pageable);
        QueryResult<CmsPage> cmsPageQueryResult = new QueryResult<CmsPage>();
        cmsPageQueryResult.setList(all.getContent());
        cmsPageQueryResult.setTotal(all.getTotalElements());
//????????????
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
//??????????????????
             one.setSiteId(cmsPage.getSiteId());
//??????????????????
             one.setPageAliase(cmsPage.getPageAliase());
//??????????????????
             one.setPageName(cmsPage.getPageName());
//??????????????????
             one.setPageWebPath(cmsPage.getPageWebPath());
//??????????????????
             one.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
             one.setDataUrl(cmsPage.getDataUrl());
//????????????
             CmsPage save = cmsPageRepository.save(one);
             if (save != null) {
//????????????
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
//????????????????????????
        Map model = this.getModelByPageId(pageId);
        if(model == null){
//??????????????????????????????
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
//??????????????????
        String templateContent = getTemplateByPageId(pageId);
        if(StringUtils.isEmpty(templateContent)){
//??????????????????
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
//???????????????
        String html = generateHtml(templateContent, model);
        if(StringUtils.isEmpty(html)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        return html;
    }
    //???????????????
    public String generateHtml(String template,Map model){
        try {
//???????????????
            Configuration configuration = new Configuration(Configuration.getVersion());
//???????????????
            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            stringTemplateLoader.putTemplate("template",template);
//?????????????????????
            configuration.setTemplateLoader(stringTemplateLoader);
//????????????
            Template template1 = configuration.getTemplate("template");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template1, model);
            return html;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //??????????????????
    public String getTemplateByPageId(String pageId){
//??????????????????
        CmsPage cmsPage = findById(pageId);
        if(cmsPage == null){
//???????????????
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
//????????????
        String templateId = cmsPage.getTemplateId();
        if(StringUtils.isEmpty(templateId)){
//??????????????????
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        Optional<CmsTemplate> optional = cmsTemplateRepository.findById(templateId);
        if(optional.isPresent()){
            CmsTemplate cmsTemplate = optional.get();
//????????????id
            String templateFileId = cmsTemplate.getTemplateFileId();
//????????????????????????
            GridFSFile gridFSFile =
                    gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
//?????????????????????
            GridFSDownloadStream gridFSDownloadStream =
                    gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
//??????GridFsResource
            GridFsResource gridFsResource = new GridFsResource(gridFSFile,gridFSDownloadStream);
            try {
                String content = IOUtils.toString(gridFsResource.getInputStream(), "utf???8");
                return content;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
        //????????????????????????
    public Map getModelByPageId(String pageId){
        //??????????????????
        CmsPage cmsPage = this.findById(pageId);
        if(cmsPage == null){
        //???????????????
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        //??????dataUrl
        String dataUrl = cmsPage.getDataUrl();
        if(StringUtils.isEmpty(dataUrl)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(dataUrl, Map.class);
        Map body = forEntity.getBody();
        return body;
    }

}

