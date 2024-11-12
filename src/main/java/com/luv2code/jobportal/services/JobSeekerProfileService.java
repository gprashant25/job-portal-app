package com.luv2code.jobportal.services;

import com.luv2code.jobportal.entity.JobSeekerProfile;
import com.luv2code.jobportal.entity.RecruiterProfile;
import com.luv2code.jobportal.entity.Users;
import com.luv2code.jobportal.repository.JobSeekerProfileRepository;
import com.luv2code.jobportal.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JobSeekerProfileService {

    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final UsersRepository usersRepository;

    @Autowired
    public JobSeekerProfileService(JobSeekerProfileRepository jobSeekerProfileRepository, UsersRepository usersRepository) {
        this.jobSeekerProfileRepository = jobSeekerProfileRepository;
        this.usersRepository = usersRepository;
    }


    // Adding a method to get a job seeker profile based on the id
    public Optional<JobSeekerProfile> getOne(Integer id) {

        return jobSeekerProfileRepository.findById(id);
    }


    // method to save a job seeker profile information to the database
    public JobSeekerProfile addNew(JobSeekerProfile jobSeekerProfile) {

        return jobSeekerProfileRepository.save(jobSeekerProfile);
    }

    public JobSeekerProfile getCurrentSeekerProfile() {

        // here we're getting that user and then the actual profile associated with that user, that job seeker profile.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(!(authentication instanceof AnonymousAuthenticationToken)) {

            String currentUsername = authentication.getName();

            Users users = usersRepository.findByEmail(currentUsername).orElseThrow(
                    () -> new UsernameNotFoundException("User not found "));

            Optional<JobSeekerProfile> seekerProfile = getOne(users.getUserId());

            return seekerProfile.orElse(null);
        }
        else {
            return null;
        }

    }
}
