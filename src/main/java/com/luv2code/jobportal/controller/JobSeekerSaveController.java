package com.luv2code.jobportal.controller;

import com.luv2code.jobportal.entity.JobPostActivity;
import com.luv2code.jobportal.entity.JobSeekerProfile;
import com.luv2code.jobportal.entity.JobSeekerSave;
import com.luv2code.jobportal.entity.Users;
import com.luv2code.jobportal.services.JobPostActivityService;
import com.luv2code.jobportal.services.JobSeekerProfileService;
import com.luv2code.jobportal.services.JobSeekerSaveService;
import com.luv2code.jobportal.services.UsersService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class JobSeekerSaveController {

    private final UsersService usersService;
    private final JobSeekerProfileService jobSeekerProfileService;
    private final JobPostActivityService jobPostActivityService;
    private final JobSeekerSaveService jobSeekerSaveService;

    public JobSeekerSaveController(UsersService usersService, JobSeekerProfileService jobSeekerProfileService, JobPostActivityService jobPostActivityService,
                               JobSeekerSaveService jobSeekerSaveService) {
        this.usersService = usersService;
        this.jobSeekerProfileService = jobSeekerProfileService;
        this.jobPostActivityService = jobPostActivityService;
        this.jobSeekerSaveService = jobSeekerSaveService;
    }


    // here adding a method for save

    @PostMapping("job-details/save/{id}")
    public String save(@PathVariable("id")int id, JobSeekerSave jobSeekerSave) {

        // below code to get a current logged in user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!(authentication instanceof AnonymousAuthenticationToken)) {

            String currentUsername = authentication.getName();

            Users user = usersService.findByEmail(currentUsername);

            Optional<JobSeekerProfile> seekerProfile = jobSeekerProfileService.getOne(user.getUserId());

            JobPostActivity jobPostActivity = jobPostActivityService.getOne(id);

            if(seekerProfile.isPresent() && jobPostActivity != null) {

                jobSeekerSave.setJob(jobPostActivity);
                jobSeekerSave.setUserId(seekerProfile.get());

            } else {
                throw new RuntimeException("User not found");
            }

            // here basically adding the list of jobs that this particular user has saved.
            jobSeekerSaveService.addNew(jobSeekerSave);
        }

        return "redirect:/dashboard/";

    }


    // below method to show a list of saved jobs

    @GetMapping("saved-jobs/")
    public String savedJobs(Model model) {

        // to get a list of saved jobs for this given user
        List<JobPostActivity> jobPost = new ArrayList<>();

        Object currentUserProfile = usersService.getCurrentUserProfile();

        List<JobSeekerSave> jobSeekerSaveList = jobSeekerSaveService.getCandidatesJob((JobSeekerProfile) currentUserProfile);

        for(JobSeekerSave jobSeekerSave : jobSeekerSaveList) {

            jobPost.add(jobSeekerSave.getJob());
        }

        model.addAttribute("jobPost", jobPost);
        model.addAttribute("user", currentUserProfile);


        return "saved-jobs";

    }
}
