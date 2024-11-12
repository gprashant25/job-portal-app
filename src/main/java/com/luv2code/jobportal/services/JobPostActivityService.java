package com.luv2code.jobportal.services;

import com.luv2code.jobportal.entity.*;
import com.luv2code.jobportal.repository.JobPostActivityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class JobPostActivityService {

    private final JobPostActivityRepository jobPostActivityRepository;

    public JobPostActivityService(JobPostActivityRepository jobPostActivityRepository) {
        this.jobPostActivityRepository = jobPostActivityRepository;
    }

    // saving a new job post activity on database
    public JobPostActivity addNew(JobPostActivity jobPostActivity) {

        return jobPostActivityRepository.save(jobPostActivity);
    }

    // method to get a list of Recruiter jobs based on that recruiter id and it'll return that list of RecruiterJobDto
    public List<RecruiterJobsDto> getRecruiterJobs(int recruiter) {

        List<IRecruiterJobs> recruiterJobsDtos = jobPostActivityRepository.getRecruiterJobs(recruiter);

        List<RecruiterJobsDto> recruiterJobsDtoList = new ArrayList<>();

        // convert the information from the database to DTOs. we'll basically construct a DTO based on information that we retrieved from the database.
        for(IRecruiterJobs rec: recruiterJobsDtos) {

            JobLocation loc = new JobLocation(rec.getLocationId(), rec.getCity(), rec.getState(), rec.getCountry());

            JobCompany comp = new JobCompany(rec.getCompanyId(), rec.getName(), "");

            recruiterJobsDtoList.add(new RecruiterJobsDto(rec.getTotalCandidates(), rec.getJob_post_id(),
                    rec.getJob_title(), loc, comp));
        }

        return recruiterJobsDtoList;

    }

    // to get the given job details
    public JobPostActivity getOne(int id) {

        return jobPostActivityRepository.findById(id).orElseThrow(()-> new RuntimeException("Job " +
                "not found"));

    }


    public List<JobPostActivity> getAll() {

        return jobPostActivityRepository.findAll();

    }

    public List<JobPostActivity> search(String job, String location, List<String> type,
                            List<String> remote, LocalDate searchDate) {

        // below here if the searchDate is null, then we'll  search without the date else will actually perform the search using the date
        return Objects.isNull(searchDate) ? jobPostActivityRepository.searchWithoutDate(job, location, remote, type) :
                jobPostActivityRepository.search(job, location, remote, type, searchDate);
    }
}
