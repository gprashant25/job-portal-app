package com.luv2code.jobportal.controller;

import com.luv2code.jobportal.entity.RecruiterProfile;
import com.luv2code.jobportal.entity.Users;
import com.luv2code.jobportal.repository.UsersRepository;
import com.luv2code.jobportal.services.RecruiterProfileService;
import com.luv2code.jobportal.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping("/recruiter-profile")
public class RecruiterProfileController {

    private final UsersRepository usersRepository;

    private final RecruiterProfileService recruiterProfileService;

    @Autowired
    public RecruiterProfileController(UsersRepository usersRepository, RecruiterProfileService recruiterProfileService) {
        this.usersRepository = usersRepository;
        this.recruiterProfileService = recruiterProfileService;
    }


    @GetMapping("/")
    public String recruiterProfile(Model model) {

        // here we get the information about the user that's logged in
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(!(authentication instanceof AnonymousAuthenticationToken)) {

            String currentUsername = authentication.getName();

            Users users = usersRepository.findByEmail(currentUsername).orElseThrow(() -> new
                    UsernameNotFoundException("Could not found user"));

            Optional<RecruiterProfile> recruiterProfile = recruiterProfileService.getOne(users.getUserId());

            if(!recruiterProfile.isEmpty()) {

                // here below we're adding the recruiter profile to the model so that the profile details can be transferred to the View page ie browser html page using Thymeleaf
                model.addAttribute("profile", recruiterProfile.get());

            }

        }

        return "recruiter_profile";

    }


    // adding the recruiter profile data: her using @RequestParam that will map to this Multipart that the file that's being uploaded
    @PostMapping("/addNew")
    public String addNew(RecruiterProfile recruiterProfile, @RequestParam("image")MultipartFile
                         multipartFile, Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(!(authentication instanceof AnonymousAuthenticationToken)) {

            String currentUsername = authentication.getName();

            Users users = usersRepository.findByEmail(currentUsername).orElseThrow(() -> new
                    UsernameNotFoundException("Could not found user"));

            // here basically associates the recruiter profile with the existing user account
            recruiterProfile.setUserId(users);
            recruiterProfile.setUserAccountId(users.getUserId());
        }

        model.addAttribute("profile", recruiterProfile);

        //here we're processing the image upload or the file upload for that recruiter profile image
        String fileName = "";

        if(!multipartFile.getOriginalFilename().equals("")) {

            fileName = StringUtils.cleanPath(Objects.requireNonNull
                    (multipartFile.getOriginalFilename()));

            recruiterProfile.setProfilePhoto(fileName);
        }

        RecruiterProfile savedUser = recruiterProfileService.addNew(recruiterProfile);

        // here we're setting up the upload directory of where we want to save the image profile
        String uploadDir = "photos/recruiter/" + savedUser.getUserAccountId();

        try {

            // read profile image from request-multipart file and we save that image on the server in directory, photos/recruiter based on that given user's name
            FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "redirect:/dashboard/";
    }
}
