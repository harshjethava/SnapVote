package net.SnapVote.snapVote.model;

import lombok.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;


@Document(collection = "user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;
    @NonNull
    @Indexed(unique = true)
    private String email;
    @NonNull
    @Indexed(unique = true)
    private String username;
    @NonNull
    private String password;
    private boolean hasVoted = false;

    @NonNull
    private String area;   // e.g. "gujarat", "rajasthan"


    private String fullName;
    private String gender;
    private String address;
    private String phoneNumber;
    private  LocalDate DOB;




}
