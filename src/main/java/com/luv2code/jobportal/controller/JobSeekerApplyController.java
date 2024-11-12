package com.luv2code.jobportal.controller;

import com.luv2code.jobportal.entity.*;
import com.luv2code.jobportal.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Controller
public class JobSeekerApplyController {

    private final JobPostActivityService jobPostActivityService;
    private final UsersService usersService;
    private final JobSeekerApplyService jobSeekerApplyService;
    private final JobSeekerSaveService jobSeekerSaveService;
    private final RecruiterProfileService recruiterProfileService;
    private final JobSeekerProfileService jobSeekerProfileService;

    @Autowired
    public JobSeekerApplyController(JobPostActivityService jobPostActivityService, UsersService usersService, JobSeekerApplyService jobSeekerApplyService, JobSeekerSaveService jobSeekerSaveService, RecruiterProfileService recruiterProfileService, JobSeekerProfileService jobSeekerProfileService) {
        this.jobPostActivityService = jobPostActivityService;
        this.usersService = usersService;
        this.jobSeekerApplyService = jobSeekerApplyService;
        this.jobSeekerSaveService = jobSeekerSaveService;
        this.recruiterProfileService = recruiterProfileService;
        this.jobSeekerProfileService = jobSeekerProfileService;
    }

    // method here for displaying the details for a given job
    @GetMapping("job-details-apply/{id}")
    public String display(@PathVariable("id")int id, Model model) {

        // to get that given job details
        JobPostActivity jobDetails = jobPostActivityService.getOne(id);

        // here we display the job details, and we'll add some additional information here or some additional code to actually get a list of job candidates that have applied for a given job
        List<JobSeekerApply> jobSeekerApplyList = jobSeekerApplyService.getJobCandidates(jobDetails);

        List<JobSeekerSave> jobSeekerSaveList = jobSeekerSaveService.getJobCandidates(jobDetails);

        // here getting a reference to a user who's currently logged in
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!(authentication instanceof AnonymousAuthenticationToken)) {

            if(authentication.getAuthorities().contains(new SimpleGrantedAuthority("Recruiter"))) {

                RecruiterProfile user = recruiterProfileService.getCurrentRecruiterProfile();

                if(user != null) {
                    model.addAttribute("applyList", jobSeekerApplyList);
                }

            } else {
                JobSeekerProfile user = jobSeekerProfileService.getCurrentSeekerProfile();

                if(user != null) {
                    boolean exists = false;
                    boolean saved = false;

                    for(JobSeekerApply jobSeekerApply : jobSeekerApplyList) {

                        if(jobSeekerApply.getUserId().getUserAccountId() == user.getUserAccountId()) {
                            exists = true;
                            break;
                        }
                    }

                    for(JobSeekerSave jobSeekerSave : jobSeekerSaveList) {

                        if(jobSeekerSave.getUserId().getUserAccountId() == user.getUserAccountId()) {
                            saved = true;
                            break;
                        }
                    }

                    // adding those flags to the actual model
                    model.addAttribute("alreadyApplied", exists);
                    model.addAttribute("alreadySaved", saved);
                }
            }
        }

        // here we create a new instance of this JobSeekerApply, basically our model attribute for that actual form data we add that to the model
        JobSeekerApply jobSeekerApply = new JobSeekerApply();
        model.addAttribute("applyJob", jobSeekerApply);


        // adding the job details to the model and we also add current user's information to the model
        model.addAttribute("jobDetails", jobDetails);
        model.addAttribute("user", usersService.getCurrentUserProfile());

        return "job-details";

    }


    // here we'll add a new mapping to actually persist the applied job or save the applied job
    // here below we'll also add parameter here for JobSeekerApply that basically contains the form dta for when the user actually applied for that given job.

    @PostMapping("job-details/apply/{id}")
    public String apply(@PathVariable("id")int id, JobSeekerApply jobSeekerApply) {

        // below here finding out the actual user that's currently logged in.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(!(authentication instanceof AnonymousAuthenticationToken)) {

            String currentUsername = authentication.getName();

            Users user = usersService.findByEmail(currentUsername);

            Optional<JobSeekerProfile> seekerProfile = jobSeekerProfileService.getOne(user.getUserId());

            JobPostActivity jobPostActivity = jobPostActivityService.getOne(id);

            if(seekerProfile.isPresent() && jobPostActivity != null) {

                jobSeekerApply = new JobSeekerApply();
                jobSeekerApply.setUserId(seekerProfile.get());
                jobSeekerApply.setJob(jobPostActivity);
                jobSeekerApply.setApplyDate(new Date());

            } else {
                throw new RuntimeException("User not found");
            }

            // here we're basically adding a new item, our new job that this candidate is applying for and below saving the jobSeeker apply
            jobSeekerApplyService.addNew(jobSeekerApply);

        }

        return "redirect:/dashboard/";

    }




}
