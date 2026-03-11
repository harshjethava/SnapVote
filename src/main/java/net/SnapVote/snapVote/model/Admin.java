package net.SnapVote.snapVote.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "admins")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    @Id
    private String id;
    @NonNull
    private String username;
    @NonNull
    private String password; // In production, always store hashed

    // Add roles or authorities if needed
    private String role = "ADMIN";

    private boolean votingStatus;

    private boolean resultStatus;

    private String winnerName;

    private String adminName;      // full name
    private String area;           // election area



}
