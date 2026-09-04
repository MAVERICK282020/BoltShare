package com.localsync.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Device device;

    @Column(name = "folder_path", nullable = false)
    private String folderPath;

    @Column(name = "folder_label")
    private String folderLabel;        // Display name e.g. "Documents"

    @Column(name = "can_read")
    private boolean canRead;

    @Column(name = "can_write")
    private boolean canWrite;

    @Column(name = "can_delete")
    private boolean canDelete;
}
