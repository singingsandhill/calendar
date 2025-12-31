package me.singingsandhill.calendar.datedate.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "menu_votes")
public class MenuVoteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private MenuJpaEntity menu;

    @Column(length = 10, nullable = false)
    private String voterName;

    protected MenuVoteJpaEntity() {
    }

    public MenuVoteJpaEntity(MenuJpaEntity menu, String voterName) {
        this.menu = menu;
        this.voterName = voterName;
    }

    public Long getId() {
        return id;
    }

    public MenuJpaEntity getMenu() {
        return menu;
    }

    public void setMenu(MenuJpaEntity menu) {
        this.menu = menu;
    }

    public String getVoterName() {
        return voterName;
    }
}
