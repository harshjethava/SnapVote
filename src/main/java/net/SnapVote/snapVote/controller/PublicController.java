package net.SnapVote.snapVote.controller;

import jakarta.servlet.http.HttpSession;
import net.SnapVote.snapVote.config.DynamicMongoService;
import net.SnapVote.snapVote.model.Admin;
import net.SnapVote.snapVote.model.User;
import net.SnapVote.snapVote.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

@Controller
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private DynamicMongoService mongoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    /* ================= REGISTER ================= */

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "user/register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {

        String area = user.getArea().toLowerCase().replaceAll("\\s+", "_");
        user.setArea(area);
        
        // Strong Password Validation
        String password = user.getPassword();
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (password == null || !password.matches(passwordRegex)) {
            model.addAttribute("error", "Password must be at least 8 characters long and include an uppercase letter, lowercase letter, digit, and special character (@$!%*?&).");
            return "user/register";
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setHasVoted(false);

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        // Check duplicate username
        Query usernameQuery = new Query(Criteria.where("username").is(user.getUsername()));
        if (mongoTemplate.exists(usernameQuery, "user")) {
            model.addAttribute("error", "Username already exists in this area.");
            return "user/register";
        }

        // Check duplicate email
        Query emailQuery = new Query(Criteria.where("email").is(user.getEmail()));
        if (mongoTemplate.exists(emailQuery, "user")) {
            model.addAttribute("error", "An account with this email address already exists in this area.");
            return "user/register";
        }

        // Age Verification (Must be >= 15)
        if (user.getDOB() == null) {
            model.addAttribute("error", "Date of Birth is required.");
            return "user/register";
        }
        int age = Period.between(user.getDOB(), LocalDate.now()).getYears();
        if (age < 15) {
            model.addAttribute("error", "You must be at least 15 years old to register.");
            return "user/register";
        }

        mongoTemplate.save(user, "user");
        return "redirect:/public/login";
    }

    /* ================= LOGIN + OTP ================= */

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("step", "login");
        return "user/login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String area,
                            @RequestParam String username,
                            @RequestParam String password,
                            HttpSession session,
                            Model model) {

        area = area.toLowerCase().replaceAll("\\s+", "_");
        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        User user = mongoTemplate.findOne(
                new Query(Criteria.where("username").is(username)),
                User.class,
                "user"
        );

        if (user == null) {
            model.addAttribute("error", "Username or Area not found");
            model.addAttribute("step", "login");
            return "user/login";
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "Incorrect password");
            model.addAttribute("step", "login");
            return "user/login";
        }

        otpService.generateAndSendOtp(username, area);

        session.setAttribute("otpUsername", username);
        session.setAttribute("area", area);

        model.addAttribute("step", "otp");
        return "user/login";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String otp,
                            HttpSession session,
                            Model model) {

        String username = (String) session.getAttribute("otpUsername");
        String area = (String) session.getAttribute("area");

        if (username == null || !otpService.validateOtp(username, otp)) {
            model.addAttribute("error", "Invalid OTP");
            model.addAttribute("step", "otp");
            return "user/login";
        }

        session.setAttribute("username", username);
        return "redirect:/public/dashboard";
    }

    /* ================= DASHBOARD ================= */

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {

        String username = (String) session.getAttribute("username");
        String area = (String) session.getAttribute("area");

        if (username == null || area == null) {
            return "redirect:/public/login";
        }

        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);

        User user = mongoTemplate.findOne(
                new Query(Criteria.where("username").is(username)),
                User.class,
                "user"
        );

        Admin admin = mongoTemplate.findOne(new Query(), Admin.class, "admins");

        model.addAttribute("user", user);
        model.addAttribute("userName", user.getFullName());
        model.addAttribute("votingOpen", admin.isVotingStatus());
        model.addAttribute("activePage", "home");

        if (admin.isResultStatus()) {
            model.addAttribute("winnerName", admin.getWinnerName());
        }

        return "user/dashboard";
    }
}
