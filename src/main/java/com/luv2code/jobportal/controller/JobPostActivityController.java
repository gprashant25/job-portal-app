package com.luv2code.jobportal.controller;


import com.luv2code.jobportal.entity.*;
import com.luv2code.jobportal.services.JobPostActivityService;
import com.luv2code.jobportal.services.JobSeekerApplyService;
import com.luv2code.jobportal.services.JobSeekerSaveService;
import com.luv2code.jobportal.services.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Controller
public class JobPostActivityController {

    private final UsersService usersService;
    private final JobPostActivityService jobPostActivityService;
    private final JobSeekerApplyService jobSeekerApplyService;
    private final JobSeekerSaveService jobSeekerSaveService;

    @Autowired
    public JobPostActivityController(UsersService usersService, JobPostActivityService jobPostActivityService, JobSeekerApplyService jobSeekerApplyService, JobSeekerSaveService jobSeekerSaveService) {
        this.usersService = usersService;
        this.jobPostActivityService = jobPostActivityService;
        this.jobSeekerApplyService = jobSeekerApplyService;
        this.jobSeekerSaveService = jobSeekerSaveService;
    }


    // below method here to search for jobs and login to the dashboard. below we're passing the request params as our search criteria
    @GetMapping("/dashboard/")
    public String searchJobs(Model model,
                             @RequestParam(value = "job", required = false) String job,
                             @RequestParam(value = "location", required = false) String location,
                             @RequestParam(value = "partTime", required = false) String partTime,
                             @RequestParam(value = "fullTime", required = false) String fullTime,
                             @RequestParam(value = "freelance", required = false) String freelance,
                             @RequestParam(value = "remoteOnly", required = false) String remoteOnly,
                             @RequestParam(value = "officeOnly", required = false) String officeOnly,
                             @RequestParam(value = "partialRemote", required = false) String partialRemote,
                             @RequestParam(value = "today", required = false) boolean today,
                             @RequestParam(value = "days7", required = false) boolean days7,
                             @RequestParam(value = "days30", required = false) boolean days30) {

        // adding these request params to the model
        model.addAttribute("partTime", Objects.equals(partTime, "Part-Time"));
        model.addAttribute("fullTime", Objects.equals(fullTime, "Full-Time"));
        model.addAttribute("freelance", Objects.equals(freelance, "Freelance"));

        model.addAttribute("remoteOnly", Objects.equals(remoteOnly, "Remote-Only"));
        model.addAttribute("officeOnly", Objects.equals(officeOnly, "Office-Only"));
        model.addAttribute("partialRemote", Objects.equals(partialRemote, "Partial-Remote"));

        model.addAttribute("today", today);
        model.addAttribute("days7", days7);
        model.addAttribute("days30", days30);

        model.addAttribute("job", job);
        model.addAttribute("location", location);

        // below are the search category flags
        LocalDate searchDate  = null;
        List<JobPostActivity> jobPost = null;
        boolean dateSearchFlag = true;
        boolean remote = true;
        boolean type = true;

        if(days30) {
            searchDate = LocalDate.now().minusDays(30);
        } else if (days7) {
            searchDate = LocalDate.now().minusDays(7);
        } else if (today) {
            searchDate = LocalDate.now();
        } else {
            dateSearchFlag = false;
        }

        if(partTime == null && fullTime == null && freelance == null) {
            partTime = "Part-Time";
            fullTime = "Full-Time";
            freelance = "Freelance";

            type = false;
        }

        if(officeOnly == null && remoteOnly == null && partialRemote == null) {
            officeOnly = "Office-Only";
            remoteOnly = "Remote-Only";
            partialRemote = "Partial-Remote";

            remote = false;
        }

        // below if any of the items not selected as the flags, then we simply get all jobs and if any of the flags are selected, then we'll search using those appropriate flags
        if(!dateSearchFlag && !remote && !type && !StringUtils.hasText(job) && !StringUtils.hasText(location)) {

            jobPost = jobPostActivityService.getAll();

        } else {
            jobPost = jobPostActivityService.search(job, location, Arrays.asList(partTime, fullTime, freelance),
                    Arrays.asList(remoteOnly, officeOnly, partialRemote), searchDate);
        }



        // getting the current user profile
        Object currentUserProfile = usersService.getCurrentUserProfile();

        // we can get the current logged in user from the SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(!(authentication instanceof AnonymousAuthenticationToken)) {

            String currentUsername = authentication.getName();

            model.addAttribute("username", currentUsername);

            // below code to make sure the current user logged in is a Recruiter or has a Recruiter role
            if(authentication.getAuthorities().contains(new SimpleGrantedAuthority
                    ("Recruiter"))) {

                // here we're retrieving a list of jobs for a given recruiter id
                List<RecruiterJobsDto> recruiterJobs = jobPostActivityService.
                        getRecruiterJobs(((RecruiterProfile)currentUserProfile).getUserAccountId());

                model.addAttribute("jobPost", recruiterJobs);

            } else {

                List<JobSeekerApply> jobSeekerApplyList = jobSeekerApplyService.getCandidatesJobs((JobSeekerProfile) currentUserProfile);

                // so here basically a list of jobs that a given candidate has saved already in the past
                List<JobSeekerSave> jobSeekerSaveList = jobSeekerSaveService.getCandidatesJob((JobSeekerProfile) currentUserProfile);

                boolean exist;
                boolean saved;

                // now here we'll actually loop through the different job posts and we'll check to see if this a job that we've either applied for or if a job that we actually saved in the past
                for(JobPostActivity jobActivity : jobPost) {

                    exist = false;
                    saved = false;

                    for(JobSeekerApply jobSeekerApply : jobSeekerApplyList) {

                        if(Objects.equals(jobActivity.getJobPostId(), jobSeekerApply.getJob().getJobPostId())) {
                            jobActivity.setIsActive(true);
                            exist = true;
                            break;
                        }
                    }

                    for(JobSeekerSave jobSeekerSave : jobSeekerSaveList) {

                        if(Objects.equals(jobActivity.getJobPostId(), jobSeekerSave.getJob().getJobPostId())) {
                            jobActivity.setIsSaved(true);
                            saved = true;
                            break;
                        }
                    }

                    if(!exist) {
                        jobActivity.setIsActive(false);
                    }

                    if(!saved) {
                        jobActivity.setIsSaved(false);
                    }

                    // here we're finally add this job post to the model
                    model.addAttribute("jobPost", jobPost);

                }

            }
        }

        model.addAttribute("user", currentUserProfile);

        return "dashboard";
    }

    // below API endpoint for the global search
    @GetMapping("/global-search/")
    public String globalSearch(Model model,
                               @RequestParam(value = "job", required = false) String job,
                               @RequestParam(value = "location", required = false) String location,
                               @RequestParam(value = "partTime", required = false) String partTime,
                               @RequestParam(value = "fullTime", required = false) String fullTime,
                               @RequestParam(value = "freelance", required = false) String freelance,
                               @RequestParam(value = "remoteOnly", required = false) String remoteOnly,
                               @RequestParam(value = "officeOnly", required = false) String officeOnly,
                               @RequestParam(value = "partialRemote", required = false) String partialRemote,
                               @RequestParam(value = "today", required = false) boolean today,
                               @RequestParam(value = "days7", required = false) boolean days7,
                               @RequestParam(value = "days30", required = false) boolean days30) {

        // adding these request params to the model
        model.addAttribute("partTime", Objects.equals(partTime, "Part-Time"));
        model.addAttribute("fullTime", Objects.equals(fullTime, "Full-Time"));
        model.addAttribute("freelance", Objects.equals(freelance, "Freelance"));

        model.addAttribute("remoteOnly", Objects.equals(remoteOnly, "Remote-Only"));
        model.addAttribute("officeOnly", Objects.equals(officeOnly, "Office-Only"));
        model.addAttribute("partialRemote", Objects.equals(partialRemote, "Partial-Remote"));

        model.addAttribute("today", today);
        model.addAttribute("days7", days7);
        model.addAttribute("days30", days30);

        model.addAttribute("job", job);
        model.addAttribute("location", location);


        // below are the search category flags
        LocalDate searchDate  = null;
        List<JobPostActivity> jobPost = null;
        boolean dateSearchFlag = true;
        boolean remote = true;
        boolean type = true;

        if(days30) {
            searchDate = LocalDate.now().minusDays(30);
        } else if (days7) {
            searchDate = LocalDate.now().minusDays(7);
        } else if (today) {
            searchDate = LocalDate.now();
        } else {
            dateSearchFlag = false;
        }

        if(partTime == null && fullTime == null && freelance == null) {
            partTime = "Part-Time";
            fullTime = "Full-Time";
            freelance = "Freelance";

            type = false;
        }

        if(officeOnly == null && remoteOnly == null && partialRemote == null) {
            officeOnly = "Office-Only";
            remoteOnly = "Remote-Only";
            partialRemote = "Partial-Remote";

            remote = false;
        }

        // below if any of the items not selected as the flags, then we simply get all jobs and if any of the flags are selected, then we'll search using those appropriate flags
        if(!dateSearchFlag && !remote && !type && !StringUtils.hasText(job) && !StringUtils.hasText(location)) {

            jobPost = jobPostActivityService.getAll();

        } else {
            jobPost = jobPostActivityService.search(job, location, Arrays.asList(partTime, fullTime, freelance),
                    Arrays.asList(remoteOnly, officeOnly, partialRemote), searchDate);
        }


        // adding the job post to the model
        model.addAttribute("jobPost", jobPost);


        return "global-search";

    }


    // method for posting a new job
    @GetMapping("/dashboard/add")
    public String addJobs(Model model) {

        //added some data to our model. i.e. adding or initializing the new job details to the model and we also add current user's information to the model
        model.addAttribute("jobPostActivity", new JobPostActivity());
        model.addAttribute("user", usersService.getCurrentUserProfile());

        return "add-jobs";
    }

    // this endpoint addNew, the form will pass in a job post activity so that we can save the new job post in the database
    @PostMapping("/dashboard/addNew")
    public String addNew(JobPostActivity jobPostActivity, Model model) {

        // business logic for adding a new job post activity
        // we want to get a handle to the current user who's logged in
        Users user = usersService.getCurrentUser();

        if(user != null) {
            jobPostActivity.setPostedById(user);
        }

        jobPostActivity.setPostedDate(new Date());

        //added some data to our model ie need to pass some data to the  html view page
        model.addAttribute("jobPostActivity", jobPostActivity);

        // saving the new job post activity to the database
        JobPostActivity saved = jobPostActivityService.addNew(jobPostActivity);

        return "redirect:/dashboard/";
    }

    // to edit a job posted by recruiter
    @PostMapping("dashboard/edit/{id}")
    public String editJob(@PathVariable("id")int id, Model model) {

        // to get that given job details
        JobPostActivity jobPostActivity = jobPostActivityService.getOne(id);

        // adding the job details to the model and we also add current user's information to the model
        model.addAttribute("jobPostActivity", jobPostActivity);
        model.addAttribute("user", usersService.getCurrentUserProfile());

        return "add-jobs";

    }


}
