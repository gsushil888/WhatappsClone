package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "privacy_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId
    private User user;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Visibility lastSeenVisibility = Visibility.CONTACTS;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Visibility profilePhotoVisibility = Visibility.EVERYONE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Visibility statusVisibility = Visibility.CONTACTS;

    @Builder.Default
    private Boolean readReceiptsEnabled = true;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Visibility groupsVisibility = Visibility.CONTACTS;

    public enum Visibility { EVERYONE, CONTACTS, NOBODY }
}