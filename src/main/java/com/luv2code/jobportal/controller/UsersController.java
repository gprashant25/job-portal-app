package com.luv2code.jobportal.controller;

import com.luv2code.jobportal.entity.Users;
import com.luv2code.jobportal.entity.UsersType;
import com.luv2code.jobportal.services.UsersService;
import com.luv2code.jobportal.services.UsersTypeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class UsersController {

    private final UsersTypeService usersTypeService;

    private final UsersService usersService;

    @Autowired
    public UsersController(UsersTypeService usersTypeService, UsersService usersService) {
        this.usersTypeService = usersTypeService;
        this.usersService = usersService;
    }

    // method to show our user registration form: this will display the register.html view page on browser
    @GetMapping("/register")
    public String register(Model model) {

        List<UsersType> usersTypes = usersTypeService.getAll();

        // using model to kind of pre-populate some basic form data that we have below.
        // Please note: Model is an interface used for transferring the data or attributes from our Business logic @Service class to the rendering View Pages.
        model.addAttribute("getAllTypes", usersTypes);
        model.addAttribute("user", new Users());

        return "register";         // this will display the register.html view page on browser

    }

    // method for creating a new user : ie to process that form for creating a new user
    @PostMapping("/register/new")
    public String userRegistration(@Valid Users users, Model model) {

        Optional<Users> optionalUsers = usersService.getUserByEmail(users.getEmail());

        // here we're checking that during the new user registration we're checking to see if the email address is already exists in the database before registering the user.
        // ANd if the email exists, we need to add error message to model and return to registration form ie register.html page
        if(optionalUsers.isPresent()) {

            model.addAttribute("error", "Email ID is already registered, try to login/register with some other email ID.");

            List<UsersType> usersTypes = usersTypeService.getAll();

            // using model to kind of pre-populate some basic form data that we have below.
            // Please note: Model is an interface used for transferring the data or attributes from our Business logic @Service class to the rendering View Pages.
            model.addAttribute("getAllTypes", usersTypes);
            model.addAttribute("user", new Users());

            return "register";
        }

        System.out.println("User::" + users);

        usersService.addNew(users);    // for adding the new users details into the database

        return "redirect:/dashboard/";
    }

    // API endpoint for login functionality
    @GetMapping("/login")
    public String login() {

        return "login";
    }

    // API endpoint for logout functionality
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication!=null) {

            new SecurityContextLogoutHandler().logout(request, response, authentication);

        }

        return "redirect:/";

    }
}
