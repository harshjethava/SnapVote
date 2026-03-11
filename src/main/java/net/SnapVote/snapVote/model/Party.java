package net.SnapVote.snapVote.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "parties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Party {

    @Id
    private String id;
    @Indexed(unique = true)
    private String name;

    private String symbol; // e.g., "Lion", "Star"
    private String base64Logo; // Base64 encoded image string (e.g. data:image/png;base64,...)
    private int voteCount = 0;


}
