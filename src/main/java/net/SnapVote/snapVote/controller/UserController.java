package net.SnapVote.snapVote.controller;

import jakarta.servlet.http.HttpSession;
import net.SnapVote.snapVote.config.DynamicMongoService;
import net.SnapVote.snapVote.model.Admin;
import net.SnapVote.snapVote.model.ContactUs;
import net.SnapVote.snapVote.model.Party;
import net.SnapVote.snapVote.model.User;
import net.SnapVote.snapVote.model.Vote;
import net.SnapVote.snapVote.service.ContactUsDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private DynamicMongoService mongoService;

    @Autowired
    private ContactUsDataService contactUsDataService;

    /* ================= MISC NAVIGATION ================= */

    @GetMapping("/about")
    public String showAboutPage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        String area = (String) session.getAttribute("area");

        if (username == null || area == null) return "redirect:/public/login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);
        User user = mongoTemplate.findOne(new Query(Criteria.where("username").is(username)), User.class, "user");
        model.addAttribute("userName", user != null ? user.getFullName() : "User");
        model.addAttribute("activePage", "about");
        return "user/about";
    }

    @GetMapping("/contact")
    public String showContactPage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        String area = (String) session.getAttribute("area");

        if (username == null || area == null) return "redirect:/public/login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);
        User user = mongoTemplate.findOne(new Query(Criteria.where("username").is(username)), User.class, "user");
        model.addAttribute("userName", user != null ? user.getFullName() : "User");
        model.addAttribute("activePage", "contact");
        return "user/contact";
    }

    @PostMapping("/submit-contact")
    public String submitContactForm(@RequestParam String name,
                                    @RequestParam String email,
                                    @RequestParam String subject,
                                    @RequestParam String message,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {

        ContactUs contactUs = new ContactUs();
        contactUs.setName(name);
        contactUs.setEmail(email);
        contactUs.setSubject(subject);
        contactUs.setMessage(message);

        String area = (String) session.getAttribute("area");
        if (area == null) return "redirect:/public/login";

        contactUsDataService.createContactUs(contactUs, area);

        redirectAttributes.addFlashAttribute("successPopup", true);
        return "redirect:/user/contact";
    }

    /* ================= USER DETAILS ================= */

    @GetMapping("/user-details")
    public String userDetails(HttpSession session, Model model) {

        String username = (String) session.getAttribute("username");
        String area = (String) session.getAttribute("area");

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        User user = mongoTemplate.findOne(
                new Query(Criteria.where("username").is(username)),
                User.class,
                "user"
        );

        model.addAttribute("user", user);
        model.addAttribute("userName", user.getFullName());
        model.addAttribute("activePage", "info");
        return "user/user-details";
    }

    @PostMapping("/update-detail")
    public String updateDetail(@RequestParam String field,
                               @RequestParam String value,
                               HttpSession session) {

        String username = (String) session.getAttribute("username");
        String area = (String) session.getAttribute("area");
        if (username == null || area == null) return "redirect:/public/login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        // Map frontend field ids to Backend User Class fields
        String backendField = field;
        if ("phone".equals(field)) backendField = "phoneNumber";

        mongoTemplate.updateFirst(
                new Query(Criteria.where("username").is(username)),
                new Update().set(backendField, value),
                "user"
        );

        return "redirect:/user/user-details";
    }

    /* ================= VOTE PAGE ================= */

    @GetMapping("/vote")
    public String showVotePage(HttpSession session, Model model) {

        String username = (String) session.getAttribute("username");
        String area = (String) session.getAttribute("area");

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        Admin admin = mongoTemplate.findOne(new Query(), Admin.class, "admins");
        User user = mongoTemplate.findOne(
                new Query(Criteria.where("username").is(username)),
                User.class,
                "user"
        );

        List<Party> parties = mongoTemplate.findAll(Party.class, "parties");

        model.addAttribute("voteStatus", admin.isVotingStatus());
        model.addAttribute("userVoted", user.isHasVoted());
        model.addAttribute("parties", parties);
        model.addAttribute("userName", user.getFullName());
        model.addAttribute("activePage", "vote");

        return "user/vote-page";
    }

    /* ================= SUBMIT VOTE ================= */

    @PostMapping("/submit-vote")
    public String submitVote(@RequestParam String partyName,
                             HttpSession session) {

        String username = (String) session.getAttribute("username");
        String area = (String) session.getAttribute("area");

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        User user = mongoTemplate.findOne(
                new Query(Criteria.where("username").is(username)),
                User.class,
                "user"
        );

        if (user.isHasVoted()) {
            return "redirect:/public/dashboard";
        }

        // Find party by incoming 'partyName'
        Party party = mongoTemplate.findOne(
                new Query(Criteria.where("name").is(partyName)),
                Party.class,
                "parties"
        );

        if (party == null) {
            return "redirect:/user/vote";
        }

        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(party.getId())),
                new Update().inc("voteCount", 1),
                "parties"
        );

        Vote vote = new Vote();
        vote.setUserId(user.getId());
        vote.setPartyId(party.getId());
        vote.setTimestamp(LocalDateTime.now());
        mongoTemplate.save(vote, "votes");

        mongoTemplate.updateFirst(
                new Query(Criteria.where("username").is(username)),
                new Update().set("hasVoted", true),
                "user"
        );

        return "redirect:/public/dashboard";
    }
}
