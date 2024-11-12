package com.luv2code.jobportal.controller;

import com.luv2code.jobportal.entity.JobSeekerProfile;
import com.luv2code.jobportal.entity.Skills;
import com.luv2code.jobportal.entity.Users;
import com.luv2code.jobportal.repository.UsersRepository;
import com.luv2code.jobportal.services.JobSeekerProfileService;
import com.luv2code.jobportal.util.FileDownloadUtil;
import com.luv2code.jobportal.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping("/job-seeker-profile")
public class JobSeekerProfileController {

    private JobSeekerProfileService jobSeekerProfileService;
    private UsersRepository usersRepository;

    @Autowired
    public JobSeekerProfileController(JobSeekerProfileService jobSeekerProfileService, UsersRepository usersRepository) {
        this.jobSeekerProfileService = jobSeekerProfileService;
        this.usersRepository = usersRepository;
    }

    // setting a method to show the job seeker profile
    @GetMapping("/")
    public String JobSeekerProfile(Model model) {

        JobSeekerProfile jobSeekerProfile = new JobSeekerProfile();

        // we can get the current logged in user from the SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        List<Skills> skills = new ArrayList<>();

        if(!(authentication instanceof AnonymousAuthenticationToken)) {

            Users user = usersRepository.findByEmail(authentication.getName()).orElseThrow(
                    () -> new UsernameNotFoundException("User not found."));

            // below we get the job seeker profile based on that user ID
            Optional<JobSeekerProfile> seekerProfile = jobSeekerProfileService.getOne(user.getUserId());

            // regarding skills code
            if(seekerProfile.isPresent()) {
                jobSeekerProfile = seekerProfile.get();

                if(jobSeekerProfile.getSkills().isEmpty()) {
                    skills.add(new Skills());

                    jobSeekerProfile.setSkills(skills);
                }
            }

            model.addAttribute("skills", skills);
            model.addAttribute("profile", jobSeekerProfile);
        }

        return "job-seeker-profile";
    }


    // adding or updating the existing job seeker profile information and saving it into the database
    // Please NOTE: when the job seeker user add his profile information in html view page and once he submit the info so when this form submission happens, we have some data that's passed in hence declaring the JobSeekerProfile parameter in the method.
    // here from form submission, we also get the file upload for the profile image and also get the file upload for the resume.
    @PostMapping("/addNew")
    public String addNew(JobSeekerProfile jobSeekerProfile,
                         @RequestParam("image")MultipartFile image,
                         @RequestParam("pdf") MultipartFile pdf, Model model) {

        // BELOW ARE: THE business logic for adding or updating a job seeker profile information and saving that info to the database.
        // Security code: we can get the current logged in user from the SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(!(authentication instanceof AnonymousAuthenticationToken)) {

            Users user = usersRepository.findByEmail(authentication.getName()).orElseThrow(
                    () -> new UsernameNotFoundException("User not found."));

            jobSeekerProfile.setUserId(user);
            jobSeekerProfile.setUserAccountId(user.getUserId());

        }

        List<Skills> skillsList = new ArrayList<>();

        model.addAttribute("profile", jobSeekerProfile);
        model.addAttribute("skills", skillsList);

        //adding the skills
        for(Skills skills : jobSeekerProfile.getSkills()) {
            skills.setJobSeekerProfile(jobSeekerProfile);
        }

        // below code for handling the file upload for the profile image and also for the resume.
        String imageName = "";
        String resumeName = "";

        // for the profile image
        if(!Objects.equals(image.getOriginalFilename(), "")) {

            imageName = StringUtils.cleanPath(Objects.requireNonNull(image.getOriginalFilename()));

            jobSeekerProfile.setProfilePhoto(imageName);
        }

        // for the actual resume
        if(!Objects.equals(pdf.getOriginalFilename(), "")) {

            resumeName = StringUtils.cleanPath(Objects.requireNonNull(pdf.getOriginalFilename()));

            jobSeekerProfile.setResume(resumeName);
        }

        // we need to save in the database
        JobSeekerProfile seekerProfile = jobSeekerProfileService.addNew(jobSeekerProfile);

        // below code for actually save the file to the file system ie. to save the profile image in our jobportal project source code photos file.
        try{

            String uploadDir = "photos/candidate/" + jobSeekerProfile.getUserAccountId();

            //here making FileUploadUtil to save the file for the profile image
            if(!Objects.equals(image.getOriginalFilename(), "")) {

                FileUploadUtil.saveFile(uploadDir, imageName, image);
            }

            // for saving the file for the resume
            if(!Objects.equals(pdf.getOriginalFilename(), "")) {

                FileUploadUtil.saveFile(uploadDir, resumeName, pdf);
            }


        }catch(IOException ex){
            throw new RuntimeException(ex);

        }

        return "redirect:/dashboard/";


    }

    // below API is basically show the profile or retrieve a profile for a given job candidate ID
    @GetMapping("/{id}")
    public String candidateProfile(@PathVariable("id")int id, Model model) {

        Optional<JobSeekerProfile> seekerProfile = jobSeekerProfileService.getOne(id);

        model.addAttribute("profile", seekerProfile.get());

        return "job-seeker-profile";
    }


    // below API endpoint to download a candidate resume.
    // Note: to download a candidate resume we need to use the file download utitlity class
    @GetMapping("/downloadResume")
    public ResponseEntity<?> downloadResume(@RequestParam(value = "fileName")String fileName,
                            @RequestParam(value = "userID")String userId) {

        FileDownloadUtil fileDownloadUtil = new FileDownloadUtil();

        Resource resource = null;

        try{
            resource = fileDownloadUtil.getFileAsResource("photos/candidate/" + userId, fileName);

        }catch (IOException io) {
            return ResponseEntity.badRequest().build();
        }

        if(resource == null) {
            return new ResponseEntity<>("File not found.", HttpStatus.NOT_FOUND);
        }

        // here we're going to send back the file to download. So we set that accordingly with the content type of application octet stream. so that way your browsere will know that, we're sending over a binary file or a stream of binary or octets
        String contentType = "application/octet-stream";
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

        // here resource is the actual contents of the file, and that will be in the response body. so the browser will get a stream of binary data and use it or save it accordingly to your local computer.

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(resource);


    }

}
