package com.spring.cswiki.controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import com.spring.cswiki.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.spring.cswiki.service.DocService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.JsonObject;


@Controller
@RequestMapping("/doc/*")

public class DocController {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Inject
    private DocService service;
    // CK에디터 파일 업로드
    @RequestMapping(value="/ckUpload", method=RequestMethod.POST)
    @ResponseBody
    public String fileUpload(HttpServletRequest req, HttpServletResponse resp, MultipartHttpServletRequest multiFile) throws Exception {
        JsonObject json = new JsonObject();
        PrintWriter printWriter = null;
        OutputStream out = null;
        MultipartFile file = multiFile.getFile("upload");
        if (file != null) {
            if (file.getSize() > 0 && StringUtils.isNotBlank(file.getName())) {
                if (file.getContentType().toLowerCase().startsWith("image/")) {
                    try {
                        String fileName = file.getName();
                        byte[] bytes = file.getBytes();
                        String uploadPath = req.getServletContext().getRealPath("/ckimage/");
                        File uploadFile = new File(uploadPath);
                        if (!uploadFile.exists()) {
                            uploadFile.mkdirs();
                        }
                        fileName = UUID.randomUUID().toString();
                        uploadPath = uploadPath + "/" + fileName;
                        out = new FileOutputStream(new File(uploadPath));
                        out.write(bytes);

                        printWriter = resp.getWriter();
                        resp.setContentType("text/html");
                        String fileUrl = req.getContextPath() + "/ckimage/" + fileName;

                        // json ???? ??
                        // {"uploaded" : 1, "fileName" : "test.jpg", "url" : "/img/test.jpg"}
                        // ?? ??? ??? ????.
                        json.addProperty("uploaded", 1);
                        json.addProperty("fileName", fileName);
                        json.addProperty("url", fileUrl);

                        printWriter.println(json);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (out != null) {
                            out.close();
                        }
                        if (printWriter != null) {
                            printWriter.close();
                        }
                    }
                }
            }
        }
        return null;
    }

    // 문서 조회
    @RequestMapping(value = "/doc", method = RequestMethod.GET)
    public String getdoc(Model model, @RequestParam(required = false) Integer d_num, @RequestParam(required = false) String d_title, HttpServletRequest request) throws Exception {
        LocalDateTime lastVisit = LocalDateTime.now();
        HttpSession session = request.getSession();

        String userId = (String)session.getAttribute("u_id");
        List<Map<String, Object>> jsonData = service.generateCategoryTreeJson();
        model.addAttribute("jsonData", jsonData);

        try{
            if(d_num != null){
                Doc doc = service.doc(d_num);
                List<Comment> comments = service.readComment(d_num);
                service.setDocTimeNum(d_num, lastVisit);
                model.addAttribute("comment", comments);
                model.addAttribute("u_id", userId);
                model.addAttribute("doc", doc);
            } else if(d_title != null){
                Doc doc = service.search(d_title);
                int docNum = doc.getD_num();
                List<Comment> comments = service.readComment(docNum);
                service.setDocTimeTitle(d_title, lastVisit);
                // log.info(String.valueOf(doc.getB_ca_name()));
                // log.info(String.valueOf(doc.getLastVisit()));
                // log.info(String.valueOf(doc.getP_read()));
                model.addAttribute("comment", comments);
                model.addAttribute("doc", doc);
                model.addAttribute("u_id", userId);
                model.addAttribute("d_title", d_title);
            }
        } catch(NullPointerException e) {
            System.out.println("NullPointerException e 발생 :" + e);
            return "doc/notfind";
        }

        return "doc/doc";
    }

    // 문서 생성창 이동
    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String getcreate(Model model) throws Exception {
        List<Category> list = service.selectSecondCategory();
        model.addAttribute("list", list);
        return "doc/create";
    }

    // 문서 생성
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String postcreate(Doc domain) throws Exception {
        log.info(String.valueOf("??? ?" + domain.getS_ca_num()));
        int result = service.create(domain);
        if(result > 0) {
            return "redirect:doc?d_num=" + domain.getD_num();
        } else {
            return "redirect:list";
        }
    }

    // 문서 제목을 이용하여 댓글 작성하기
    @PostMapping(value="/doc/comment")
    public String writeComment(String u_id, @RequestParam(required = false) Integer d_num, @RequestParam(required = false) String d_title, @RequestParam String cm_comment, Model model) {
        LocalDateTime cm_time = LocalDateTime.now();
        if (d_num != null) {
            Doc doc = service.doc(d_num);
            service.writeComment(u_id, d_num, cm_comment, cm_time);
            log.info(u_id);
            log.info(cm_comment);
        } else if (d_title != null) {
            Doc doc = service.search(d_title);
            int docNum = doc.getD_num();
            service.writeComment(u_id, docNum, cm_comment, cm_time);
            log.info(u_id);
            log.info(cm_comment);
        }
        String script = "parent.location.reload();";
        model.addAttribute("script", script);
        return "doc/_execute_script";
    }

    @PostMapping("/comment/update")
    public String updateComment(@RequestParam Integer cm_num, @RequestParam String cm_comment, Model model) {
        Comment selectedComment = service.selectComment(cm_num);
        Doc doc = service.doc(selectedComment.getD_num());
        Comment comment = service.selectComment(cm_num);
        LocalDateTime cm_time = LocalDateTime.now();
        String u_id = comment.getU_id();
        int d_num = comment.getD_num();
        service.updateComment(cm_num, u_id, cm_comment, cm_time);
        log.info(u_id);
        log.info(cm_comment);
        String script = "parent.location.reload();";
        model.addAttribute("script", script);
        return "doc/_execute_script";
    }

    // 댓글 삭제
    @GetMapping("/comment/delete/{cm_num}")
    public String deleteComment(@PathVariable Integer cm_num, Model model, HttpSession session) {
        // 삭제 처리
        Member member = (Member)session.getAttribute("member");
        Comment comment = service.selectComment(cm_num);
        service.deleteComment(cm_num, member.getU_id(), comment.getD_num());

        // 삭제 완료 후 현재 페이지 새로고침 후 마지막 위치로 이동
        String script = "parent.location.reload();";
        model.addAttribute("script", script);
        return "doc/_execute_script";
    }
    // 문서 편집창 이동
    @RequestMapping(value= "/edit", method = RequestMethod.GET)
    public String getedit(int d_num, Model model) throws Exception{
        Doc doc = service.doc(d_num);
        List<Category> list = service.selectSecondCategory();
        Category idx = service.getByCategoryId(d_num);
        model.addAttribute("list", list);
        model.addAttribute("doc", doc);
        model.addAttribute("idx", idx);
        return "doc/edit";
    }

    // 문서 편집
    @RequestMapping(value="/edit", method=RequestMethod.POST)
    public String postedit(Doc domain) throws Exception{
        int result = service.edit(domain);
        List<Star> users = service.starUsers(domain.getD_num());
        if(result > 0) {
            service.editDoc(domain.getD_num());
            return "redirect:doc?d_num=" + domain.getD_num();
        } else {
            return "redirect:list";
        }
    }

    // 문서 삭제
    @RequestMapping(value="/delete", method=RequestMethod.GET)
    public String postdelete(int d_num) throws Exception{
        service.delete(d_num);
        return "redirect:/";
    }

    // 접근 제한 페이지 이동
    @RequestMapping(value="/acl", method=RequestMethod.GET)
    public String getacl(Model model, int d_num) throws Exception{
        Doc doc = service.doc(d_num);
        model.addAttribute("doc", doc);
        return "doc/acl";
    }

    // 접근 제한 설정
    @RequestMapping(value="/acl", method=RequestMethod.POST)
    public String postacl(Doc domain, int d_num) throws Exception{
        service.acl(domain);
        return "redirect:acl?d_num=" + d_num;
    }

    // 문서 역사 조회
    @RequestMapping("/doc_history")
    public String getDocumentHistory(@RequestParam("d_num") int d_num, Model model) {
        List<DocHistory> historyList = service.getDocHistory(d_num);
        Doc doc = service.doc(d_num);
        model.addAttribute("historyList", historyList);
        System.out.println(historyList);
        model.addAttribute("doc", doc);
        return "doc/doc_history";
    }

    // 버전 별 문서 조호
    @RequestMapping("/doc_version")
    public String version(@RequestParam("d_num") int d_num, @RequestParam("d_version") String d_version, Model model) {
        Doc version = service.version(d_num, d_version);
        model.addAttribute("doc", version);
        return "doc/doc_version";
    }

    // 즐겨찾기 등록
    @RequestMapping(value="/starcheck")
    public String starin(Model model, Star vo, int d_num) {
        Doc doc = service.doc(d_num);
        LocalDateTime docDate = doc.getLastVisit();
        log.info(String.valueOf(docDate));
        model.addAttribute("doc", doc);
        service.starin(vo);
        return "redirect:doc?d_num=" + d_num;
    }

    // 즐겨찾기 삭제
    @RequestMapping(value="/starout")
    @ResponseBody
    public Map<String, Object> starout(@RequestParam("num")int d_num, @RequestParam("id")String id){
       int result = service.starout(d_num, id);
       Map<String, Object> map = new HashMap<>();
       if(result == 1){
           map.put("code", 200);
       }
       return map;
    }

    // 사용자 즐겨찾기 목록 조회
    @RequestMapping(value="/userstar")
    public String userstar(Model model, @RequestParam("u_id") String u_id) throws Exception{
        List<Doc> star = service.userstar(u_id);

        Timestamp timestamp;
        for(int i = 0; i < star.size(); i++) {
            Doc doc = star.get(i);
            String docTitle = doc.getD_title();
            timestamp = service.getDocTimeTitle(docTitle);
            LocalDateTime lastVisit = timestamp.toLocalDateTime();
            doc.setLastVisit(lastVisit);
        }
        model.addAttribute("star", star);
        model.addAttribute("u_id", u_id);
        return "doc/userstar";
    }

    // 인기 있는 문서 조회
    @GetMapping(value="/popular")
    public String popular(Model model) throws Exception{
        List<Doc> list = service.popular();
        model.addAttribute("list", list);
        return "doc/popular";
    }
}
