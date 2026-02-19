package com.aavita.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site {

    @Id
    @Column(name = "site_id", columnDefinition = "uuid")
    private UUID siteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String username;

    @Column(nullable = false, length = 250)
    private String location;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Device> devices = new ArrayList<>();

    public void setUser(User user) { this.user = user; }
    public User getUser() { return user; }
    public String getUsername() { return username; }
    public void setCreatedOn(Instant createdOn) { this.createdOn = createdOn; }

    @PrePersist
    protected void onCreate() {
        if (siteId == null) siteId = UUID.randomUUID();
        if (createdOn == null) createdOn = Instant.now();
    }
}
