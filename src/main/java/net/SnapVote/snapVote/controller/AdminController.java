package net.SnapVote.snapVote.controller;

import jakarta.servlet.http.HttpSession;
import net.SnapVote.snapVote.config.DynamicMongoService;
import net.SnapVote.snapVote.model.*;
import net.SnapVote.snapVote.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private DynamicMongoService mongoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    /* =========================
       ADMIN REGISTER
       ========================= */

    @GetMapping("/admin-register")
    public String showAdminRegister() {
        return "admin/admin-register";
    }

    @PostMapping("/admin-register")
    public String registerAdmin(@ModelAttribute Admin admin, 
                                @RequestParam(required = false) String existingAdminUsername,
                                @RequestParam(required = false) String existingAdminPassword,
                                Model model) {

        String area = admin.getArea().toLowerCase().replaceAll("\\s+", "_");
        admin.setArea(area);

        // Strong Password Validation
        String password = admin.getPassword();
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (password == null || !password.matches(passwordRegex)) {
            model.addAttribute("error", "Password must be at least 8 characters long and include an uppercase letter, lowercase letter, digit, and special character (@$!%*?&).");
            return "admin/admin-register";
        }

        admin.setPassword(passwordEncoder.encode(password));

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        Query q = new Query(Criteria.where("username").is(admin.getUsername()));
        if (mongoTemplate.exists(q, "admins")) {
            model.addAttribute("error", "Admin already exists for this area");
            return "admin/admin-register";
        }

        // Check if other admins exist in this area to sync their current state
        Admin existingAdmin = mongoTemplate.findOne(new Query(), Admin.class, "admins");
        if (existingAdmin != null) {
            
            // SECURITY CHECK: Attempting to join an existing area
            if (existingAdminUsername == null || existingAdminUsername.isEmpty() ||
                existingAdminPassword == null || existingAdminPassword.isEmpty()) {
                model.addAttribute("error", "Area exists! You must provide an existing admin's credentials to join.");
                return "admin/admin-register";
            }

            // Authenticate provided existing credentials
            Query authQuery = new Query(Criteria.where("username").is(existingAdminUsername));
            Admin authAdmin = mongoTemplate.findOne(authQuery, Admin.class, "admins");

            if (authAdmin == null || !passwordEncoder.matches(existingAdminPassword, authAdmin.getPassword())) {
                model.addAttribute("error", "Authorization Failed! Invalid existing admin credentials provided.");
                return "admin/admin-register";
            }

            // Sync States
            admin.setVotingStatus(existingAdmin.isVotingStatus());
            admin.setResultStatus(existingAdmin.isResultStatus());
            admin.setWinnerName(existingAdmin.getWinnerName());
        } else {
            // First admin in this area, default to false/null
            admin.setVotingStatus(false);
            admin.setResultStatus(false);
            admin.setWinnerName(null);
        }

        mongoTemplate.save(admin, "admins");

        if (!mongoTemplate.collectionExists("user")) mongoTemplate.createCollection("user");
        if (!mongoTemplate.collectionExists("parties")) mongoTemplate.createCollection("parties");
        if (!mongoTemplate.collectionExists("votes")) mongoTemplate.createCollection("votes");
        if (!mongoTemplate.collectionExists("contactUs")) mongoTemplate.createCollection("contactUs");

        model.addAttribute("msg", "Admin registered & database created successfully");
        return "admin/admin-register";
    }

    /* =========================
       ADMIN LOGIN
       ========================= */

    @GetMapping("/admin-login")
    public String showAdminLogin() {
        return "admin/admin-login";
    }

    @PostMapping("/admin-login")
    public String adminLogin(@RequestParam String area,
                             @RequestParam String username,
                             @RequestParam String password,
                             HttpSession session,
                             Model model) {

        area = area.toLowerCase().replaceAll("\\s+", "_");
        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        Query q = new Query(Criteria.where("username").is(username));
        Admin admin = mongoTemplate.findOne(q, Admin.class, "admins");

        if (admin == null || !passwordEncoder.matches(password, admin.getPassword())) {
            model.addAttribute("error", "Invalid admin credentials");
            return "admin/admin-login";
        }

        session.setAttribute("adminUsername", username);
        session.setAttribute("area", area);

        return "redirect:/admin/admin-dashboard";
    }

    /* =========================
       ADMIN DASHBOARD
       ========================= */

    @GetMapping("/admin-dashboard")
    public String adminDashboard(HttpSession session, Model model) {

        String username = (String) session.getAttribute("adminUsername");
        String area = (String) session.getAttribute("area");

        if (username == null || area == null) {
            return "redirect:/admin/admin-login";
        }

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        Admin admin = mongoTemplate.findOne(
                new Query(Criteria.where("username").is(username)),
                Admin.class, "admins");

        if (admin == null) {
            return "redirect:/admin/admin-login";
        }

        long totalUsers = mongoTemplate.count(new Query(), "user");
        long usersVoted = mongoTemplate.count(
                new Query(Criteria.where("hasVoted").is(true)), "user");

        model.addAttribute("admin", admin);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("usersVoted", usersVoted);
        model.addAttribute("usersNotVoted", totalUsers - usersVoted);

        model.addAttribute("usersVotedPercent",
                totalUsers == 0 ? 0 : (usersVoted * 100.0f) / totalUsers);
        model.addAttribute("usersNotVotedPercent",
                totalUsers == 0 ? 0 : 100 - ((usersVoted * 100.0f) / totalUsers));

        // 🔹 PARTIES + VOTE PERCENTAGE
        List<Party> parties = mongoTemplate.findAll(Party.class, "parties");

        long totalVotes = parties.stream()
                .mapToLong(Party::getVoteCount)
                .sum();

        List<Map<String, Object>> partyStats = new ArrayList<>();

        for (Party p : parties) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", p.getName());
            map.put("voteCount", p.getVoteCount());

            double percent = totalVotes == 0
                    ? 0
                    : (p.getVoteCount() * 100.0) / totalVotes;

            map.put("votePercent", String.format("%.2f", percent));
            partyStats.add(map);
        }

        model.addAttribute("totalParties", parties.size());
        model.addAttribute("parties", partyStats);
        model.addAttribute("activePage", "home");


        if (admin.isResultStatus() && admin.getWinnerName() != null) {
            model.addAttribute("winnerDeclared", true);
            model.addAttribute("winnerName", admin.getWinnerName());
        }

        return "admin/admin-dashboard";
    }

    /* =========================
       USER LIST
       ========================= */

    @GetMapping("/All-user")
    public String showAllUsers(HttpSession session, Model model) {

        String area = (String) session.getAttribute("area");
        if (area == null) return "redirect:/admin/admin-login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);
        model.addAttribute("users", mongoTemplate.findAll(User.class, "user"));
        model.addAttribute("activePage", "users");

        return "admin/All-user";
    }

    /* =========================
       CONTACT MESSAGES & ABOUT
       ========================= */

    @GetMapping("/about")
    public String showAboutPage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("adminUsername");
        String area = (String) session.getAttribute("area");

        if (username == null || area == null) return "redirect:/admin/admin-login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);
        Admin admin = mongoTemplate.findOne(
                new Query(Criteria.where("username").is(username)),
                Admin.class, "admins");

        model.addAttribute("admin", admin);
        model.addAttribute("activePage", "about");
        return "admin/about";
    }

    @GetMapping("/contact-messages")
    public String showContactMessages(HttpSession session, Model model) {

        String area = (String) session.getAttribute("area");
        if (area == null) return "redirect:/admin/admin-login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);
        List<ContactUs> messages = mongoTemplate.findAll(ContactUs.class, "contactUs");
        
        // Reverse to show latest first
        Collections.reverse(messages);
        
        model.addAttribute("messages", messages);
        model.addAttribute("activePage", "contact");

        return "admin/contact-messages";
    }

    /* =========================
       START / STOP VOTING
       ========================= */

    @GetMapping("/toggle-vote")
    public String toggleVoteForm(HttpSession session, Model model) {

        String username = (String) session.getAttribute("adminUsername");
        String area = (String) session.getAttribute("area");

        if (username == null || area == null) return "redirect:/admin/admin-login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);
        Admin admin = mongoTemplate.findOne(
                new Query(Criteria.where("username").is(username)),
                Admin.class, "admins");

        model.addAttribute("voteStatus", admin.isVotingStatus());
        model.addAttribute("activePage", "voting");
        return "admin/toggle-vote";
    }

    @PostMapping("/toggle-vote")
    public String toggleVote(@RequestParam boolean status, HttpSession session) {

        String username = (String) session.getAttribute("adminUsername");
        String area = (String) session.getAttribute("area");

        if (username == null || area == null) return "redirect:/admin/admin-login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        Update update = new Update()
                .set("votingStatus", status)
                .set("resultStatus", false)
                .set("winnerName", null);

        // Update ALL admins in this area to stay in sync
        mongoTemplate.updateMulti(new Query(), update, "admins");

        return "redirect:/admin/admin-dashboard";
    }

    @PostMapping("/remind-users")
    public String remindUsers(HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("adminUsername");
        String area = (String) session.getAttribute("area");

        if (username == null || area == null) return "redirect:/admin/admin-login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);
        
        // Find users who haven't voted
        Query query = new Query(Criteria.where("hasVoted").is(false));
        List<User> nonVoters = mongoTemplate.find(query, User.class, "user");
        
        if (!nonVoters.isEmpty()) {
            String[] emails = nonVoters.stream()
                .map(User::getEmail)
                .toArray(String[]::new);
                
            emailService.sendMail(emails, "Voting Reminder", "Dear User,\n\nPlease remember to cast your vote! Your opinion matters.\n\nThank you,\nSnapVote Admin");
            redirectAttributes.addFlashAttribute("successMsg", "Reminder emails sent successfully to " + emails.length + " non-voters!");
        } else {
            redirectAttributes.addFlashAttribute("infoMsg", "All users have already voted!");
        }

        return "redirect:/admin/toggle-vote";
    }

    /* =========================
       RESULT DECLARATION
       ========================= */

    @PostMapping("/show-result")
    public String showResult(HttpSession session) {

        String username = (String) session.getAttribute("adminUsername");
        String area = (String) session.getAttribute("area");

        if (username == null || area == null) return "redirect:/admin/admin-login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        List<Party> parties = mongoTemplate.findAll(Party.class, "parties");
        Party winner = parties.stream()
                .max(Comparator.comparingLong(Party::getVoteCount))
                .orElse(null);

        mongoTemplate.updateMulti(new Query(), new Update().set("hasVoted", false), "user");
        mongoTemplate.remove(new Query(), Vote.class, "votes");

        mongoTemplate.updateMulti(
                new Query(),
                new Update().set("voteCount", 0),
                "parties");

        // Update ALL admins in this area to stay in sync
        mongoTemplate.updateMulti(
                new Query(),
                new Update()
                        .set("resultStatus", true)
                        .set("votingStatus", false)
                        .set("winnerName", winner != null ? winner.getName() : null),
                "admins");

        return "redirect:/admin/admin-dashboard";
    }

    /* =========================
       PARTY MANAGEMENT
       ========================= */

    @GetMapping("/add-party")
    public String showAddPartyForm(HttpSession session, Model model) {

        String username = (String) session.getAttribute("adminUsername");
        String area = (String) session.getAttribute("area");

        if (username == null || area == null) {
            return "redirect:/admin/admin-login";
        }

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        Admin admin = mongoTemplate.findOne(
                new Query(Criteria.where("username").is(username)),
                Admin.class,
                "admins"
        );

        // ✅ ALWAYS set votingOpen (never null)
        boolean votingOpen = admin != null && admin.isVotingStatus();
        model.addAttribute("votingOpen", votingOpen);
        model.addAttribute("activePage", "add-party");

        return "admin/add-party";
    }


    @PostMapping("/add-party")
    public String addParty(@RequestParam("partyName") String partyName,
                           @RequestParam("partySymbol") String partySymbol,
                           @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                           HttpSession session,
                           Model model) {

        String area = (String) session.getAttribute("area");
        if (area == null) return "redirect:/admin/admin-login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        if (mongoTemplate.exists(
                new Query(Criteria.where("name").is(partyName)), "parties")) {
            model.addAttribute("error", "Party already exists");
            return "admin/add-party";
        }

        Party party = new Party();
        party.setName(partyName);
        party.setSymbol(partySymbol);
        party.setVoteCount(0);

        try {
            if (logoFile != null && !logoFile.isEmpty()) {
                byte[] bytes = logoFile.getBytes();
                String base64Image = Base64.getEncoder().encodeToString(bytes);
                String contentType = logoFile.getContentType();
                party.setBase64Logo("data:" + contentType + ";base64," + base64Image);
            }
        } catch (IOException e) {
            model.addAttribute("error", "Failed to upload logo.");
            return "admin/add-party";
        }

        mongoTemplate.save(party, "parties");
        return "redirect:/admin/admin-dashboard";
    }

    @GetMapping("/party-detail")
    public String partyDetail(HttpSession session, Model model) {

        String username = (String) session.getAttribute("adminUsername");
        String area = (String) session.getAttribute("area");
        if (username == null || area == null) return "redirect:/admin/admin-login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);
        model.addAttribute("parties", mongoTemplate.findAll(Party.class, "parties"));
        
        Admin admin = mongoTemplate.findOne(
            new Query(Criteria.where("username").is(username)), Admin.class, "admins"
        );
        if (admin != null) {
            model.addAttribute("winnerDeclared", admin.isResultStatus());
        }
        model.addAttribute("activePage", "party-detail");

        return "admin/party-detail";
    }

    @PostMapping("/delete-party")
    public String deleteParty(@RequestParam String partyName,
                              HttpSession session,
                              Model model) {

        String area = (String) session.getAttribute("area");
        if (area == null) return "redirect:/admin/admin-login";

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);
        mongoTemplate.remove(
                new Query(Criteria.where("name").is(partyName)),
                "parties");

        return "redirect:/admin/party-detail";
    }

    /* =========================
       LOGOUT
       ========================= */

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/admin-login";
    }
}
