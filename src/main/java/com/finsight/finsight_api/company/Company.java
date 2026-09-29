package com.finsight.finsight_api.company;

import com.finsight.finsight_api.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_companies_users"))
    private User user;

    @NotBlank
    @Size(min = 2, max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank
    @Size(min = 3, max = 100)
    @Column(nullable = false, length = 100)
    private String sector;

    /**
     * Optional, but unique among active companies when present (several NULLs may coexist).
     * Enforced by the partial index uk_companies_tax_id ON companies (tax_id) WHERE active,
     * so a soft-deleted company releases its taxId.
     */
    @Size(max = 50)
    @Column(name = "tax_id", length = 50)
    private String taxId;

    /** Soft delete flag: DELETE marks the company inactive instead of erasing it and its transactions. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}