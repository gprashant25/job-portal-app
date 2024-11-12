package com.luv2code.jobportal.services;

import com.luv2code.jobportal.entity.JobSeekerProfile;
import com.luv2code.jobportal.entity.RecruiterProfile;
import com.luv2code.jobportal.entity.Users;
import com.luv2code.jobportal.repository.JobSeekerProfileRepository;
import com.luv2code.jobportal.repository.RecruiterProfileRepository;
import com.luv2code.jobportal.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class UsersService {

    private final UsersRepository usersRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsersService(UsersRepository usersRepository, JobSeekerProfileRepository jobSeekerProfileRepository, RecruiterProfileRepository recruiterProfileRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.jobSeekerProfileRepository = jobSeekerProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }


    // Creating a method for adding/saving a new user into the database
    public Users addNew(Users users) {

        users.setActive(true);
        users.setRegistrationDate(new Date(System.currentTimeMillis()));

        // saving the users password in the encrypted format
        users.setPassword(passwordEncoder.encode(users.getPassword()));

        // saving the users details into the database
        Users savedUser = usersRepository.save(users);

        // checking whether the new user is recruiter or jobSeeker
        int userTypeId = users.getUserTypeId().getUserTypeId();
        if(userTypeId == 1) {
            recruiterProfileRepository.save(new RecruiterProfile(savedUser));
        } else {
            jobSeekerProfileRepository.save(new JobSeekerProfile(savedUser));
        }

        return savedUser;
    }

    public Optional<Users> getUserByEmail(String email) {

        return usersRepository.findByEmail(email);
    }

    public Object getCurrentUserProfile() {

        // we can get the current logged in user from the SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // here we're making sure we're not anonymous we get the actual username from the authentication.
        if(!(authentication instanceof AnonymousAuthenticationToken)) {

            String username = authentication.getName();
            Users users = usersRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Could not found user"));

            int userId = users.getUserId();

            // to display the users profile once authenticated and checks whether logged in user is recruiter profile or job seeker profile
            if(authentication.getAuthorities().contains(new SimpleGrantedAuthority
                    ("Recruiter"))) {

                RecruiterProfile recruiterProfile = recruiterProfileRepository.findById(userId)
                        .orElse(new RecruiterProfile());

                return recruiterProfile;

            } else {

                JobSeekerProfile jobSeekerProfile = jobSeekerProfileRepository.findById(userId)
                        .orElse(new JobSeekerProfile());

                return jobSeekerProfile;
            }
        }

        return null;
    }

    // below method to get the current user detail
    public Users getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(!(authentication instanceof AnonymousAuthenticationToken)) {

            String username = authentication.getName();

            Users user = usersRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Could not found user"));

            return user;
        }

        return null;
    }


    public Users findByEmail(String currentUsername) {

        return usersRepository.findByEmail(currentUsername).orElseThrow(
                () -> new UsernameNotFoundException("User not found"));
    }
}
