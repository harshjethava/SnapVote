package net.SnapVote.snapVote.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "contactUs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactUs {

    @Id
    private String Id;

    private String name;

    @NonNull
    private String email;

    private String subject;
    private String message;


}
