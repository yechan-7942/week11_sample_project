package edu.handong.web11.controller;

import edu.handong.web11.domain.Post;
import edu.handong.web11.dto.PostForm;
import edu.handong.web11.service.PostService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class PostController {
    private final PostService posts;

    public PostController(PostService posts) {
        this.posts = posts;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/posts";
    }

    @GetMapping("/posts")
    public String list(Model model) {
        model.addAttribute("posts", posts.all());
        return "posts/list";
    }

    @GetMapping("/posts/new")
    public String form(Model model) {
        model.addAttribute("postForm", new PostForm());
        return "posts/form";
    }

    // 작성 화면에서 저장 버튼을 누르면 실행됨
    @PostMapping("/posts")
    // 입력한 제목과 내용을 form으로 받고, 빈칸이나 제목 길이 오류는 errors에 담는다.
    // 첨부한 파일들은 files로 받는다. 파일을 첨부하지 않아도 된다.
    public String create(@Valid @ModelAttribute("postForm") PostForm form, BindingResult errors,
                         @RequestParam(name = "files", required = false) List<MultipartFile> files) {
        // 입력에 문제가 있으면 저장하지 않고 작성 화면을 다시 보여준다.
        if (errors.hasErrors()) return "posts/form";
        // 제목·내용과 첨부파일의 저장을 서비스에 맡기고, 새 게시글 번호를 받는다.
        // 첨부파일이 없으면 빈 목록(List.of())을 전달한다.
        Long id = posts.create(form, files == null ? List.of() : files);
        // 저장한 게시글의 상세 화면으로 이동한다. 새로고침해도 같은 글이 다시 저장되지 않게 한다.
        return "redirect:/posts/" + id;
    }

    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("post", posts.get(id));
        return "posts/detail";
    }

    @GetMapping("/posts/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Post post = posts.get(id);
        PostForm form = new PostForm();
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());
        model.addAttribute("postForm", form);
        model.addAttribute("postId", id);
        return "posts/form";
    }

    @PostMapping("/posts/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("postForm") PostForm form, BindingResult errors,
                         Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("postId", id);
            return "posts/form";
        }
        posts.update(id, form);
        return "redirect:/posts/" + id;
    }

    @PostMapping("/posts/{id}/delete")
    public String delete(@PathVariable Long id) {
        posts.delete(id);
        return "redirect:/posts";
    }
}
