-- Initialize Sequences
CREATE SEQUENCE OPPFOELGINGSDIALOG_ID_SEQ START 1 INCREMENT BY 1;
CREATE SEQUENCE GODKJENTPLAN_ID_SEQ START 1 INCREMENT BY 1;
CREATE SEQUENCE ARBEIDSOPPGAVE_ID_SEQ START 1 INCREMENT BY 1;
CREATE SEQUENCE TILTAK_ID_SEQ START 1 INCREMENT BY 1;
CREATE SEQUENCE GJENNOMFOERING_ID_SEQ START 1 INCREMENT BY 1;
CREATE SEQUENCE GODKJENNING_ID_SEQ START 1 INCREMENT BY 1;
CREATE SEQUENCE KOMMENTAR_ID_SEQ START 1 INCREMENT BY 1;

-- Create Tables
CREATE TABLE OPPFOELGINGSDIALOG
(
    oppfoelgingsdialog_id       BIGINT      NOT NULL DEFAULT nextval('OPPFOELGINGSDIALOG_ID_SEQ') PRIMARY KEY,
    aktoer_id                   VARCHAR(13) NOT NULL,
    uuid                        UUID,
    virksomhetsnummer           VARCHAR(9)  NOT NULL,
    opprettet_av                VARCHAR(13) NOT NULL,
    created                     TIMESTAMP   NOT NULL,
    arbeidsgiver_sist_innlogget TIMESTAMP,
    sykmeldt_sist_innlogget     TIMESTAMP,
    arbeidsgiver_sist_aksessert TIMESTAMP,
    sykmeldt_sist_aksessert     TIMESTAMP,
    arbeidsgiver_sist_endret    TIMESTAMP,
    sykmeldt_sist_endret        TIMESTAMP,
    sist_endret_av              VARCHAR(13) NOT NULL,
    sist_endret                 TIMESTAMP   NOT NULL,
    samtykke_sykmeldt           BOOLEAN,
    samtykke_arbeidsgiver       BOOLEAN,
    sm_fnr                      VARCHAR(11),
    opprettet_av_fnr            VARCHAR(11),
    sist_endret_av_fnr          VARCHAR(11)
);

CREATE TABLE GODKJENNING
(
    godkjenning_id        BIGINT      NOT NULL DEFAULT nextval('GODKJENNING_ID_SEQ') PRIMARY KEY,
    oppfoelgingsdialog_id BIGINT      NOT NULL,
    aktoer_id             VARCHAR(13) NOT NULL,
    godkjent              BOOLEAN     NOT NULL,
    beskrivelse           TEXT,
    fom                   TIMESTAMP,
    tom                   TIMESTAMP,
    del_med_nav           BOOLEAN,
    evalueres             TIMESTAMP,
    created               TIMESTAMP   NOT NULL,
    CONSTRAINT GODKJENNING_PLAN_FK FOREIGN KEY (oppfoelgingsdialog_id) REFERENCES OPPFOELGINGSDIALOG (oppfoelgingsdialog_id)
);

CREATE TABLE GODKJENTPLAN
(
    godkjentplan_id             BIGINT              NOT NULL DEFAULT nextval('GODKJENTPLAN_ID_SEQ') PRIMARY KEY,
    oppfoelgingsdialog_id       BIGINT              NOT NULL UNIQUE,
    dokument_uuid               VARCHAR(100) UNIQUE NOT NULL,
    versjon                     INTEGER             NOT NULL,
    sak_id                      VARCHAR(20),
    journalpost_id              VARCHAR(20),
    samtykke_sykmeldt           BOOLEAN             NOT NULL,
    samtykke_arbeidsgiver       BOOLEAN             NOT NULL,
    created                     TIMESTAMP           NOT NULL,
    fom                         TIMESTAMP           NOT NULL,
    tom                         TIMESTAMP           NOT NULL,
    evalueres                   TIMESTAMP           NOT NULL,
    avbrutt_tidspunkt           TIMESTAMP,
    avbrutt_av                  VARCHAR(13)         NOT NULL,
    delt_med_nav_tidspunkt      TIMESTAMP,
    tvungen_godkjenning         BOOLEAN             NOT NULL,
    delt_med_nav                BOOLEAN             NOT NULL,
    delt_med_fastlege_tidspunkt TIMESTAMP,
    delt_med_fastlege           BOOLEAN             NOT NULL,
    tildelt_enhet               VARCHAR(10),
    CONSTRAINT OPPFO_GODKJENTPLAN_FK FOREIGN KEY (oppfoelgingsdialog_id) REFERENCES OPPFOELGINGSDIALOG (oppfoelgingsdialog_id)
);

CREATE TABLE ARBEIDSOPPGAVE
(
    arbeidsoppgave_id      BIGINT       NOT NULL DEFAULT nextval('ARBEIDSOPPGAVE_ID_SEQ') PRIMARY KEY,
    oppfoelgingsdialog_id  BIGINT       NOT NULL,
    navn                   VARCHAR(120) NOT NULL,
    er_vurdert_av_sykmeldt BOOLEAN,
    opprettet_av           VARCHAR(13)  NOT NULL,
    sist_endret_av         VARCHAR(13)  NOT NULL,
    sist_endret_dato       TIMESTAMP    NOT NULL,
    opprettet_dato         TIMESTAMP    NOT NULL,
    paa_annet_sted         BOOLEAN,
    med_mer_tid            BOOLEAN,
    med_hjelp              BOOLEAN,
    beskrivelse            TEXT,
    kan_ikke_beskrivelse   TEXT,
    gjennomfoering_status  VARCHAR(30),
    CONSTRAINT OPPFO_ARBOPG_FK FOREIGN KEY (oppfoelgingsdialog_id) REFERENCES OPPFOELGINGSDIALOG (oppfoelgingsdialog_id)
);

CREATE TABLE TILTAK
(
    tiltak_id                BIGINT       NOT NULL DEFAULT nextval('TILTAK_ID_SEQ') PRIMARY KEY,
    oppfoelgingsdialog_id    BIGINT       NOT NULL,
    navn                     VARCHAR(120) NOT NULL,
    fom                      DATE,
    tom                      DATE,
    beskrivelse              TEXT,
    beskrivelse_ikke_aktuelt BYTEA,
    status                   VARCHAR(13)           DEFAULT 'FORSLAG' NOT NULL,
    gjennomfoering           TEXT,
    opprettet_av             VARCHAR(13)  NOT NULL,
    sist_endret_av           VARCHAR(13)  NOT NULL,
    sist_endret_dato         TIMESTAMP    NOT NULL,
    opprettet_dato           TIMESTAMP    NOT NULL,
    CONSTRAINT OPPFO_TILTAK_FK FOREIGN KEY (oppfoelgingsdialog_id) REFERENCES OPPFOELGINGSDIALOG (oppfoelgingsdialog_id)
);

CREATE TABLE DOKUMENT
(
    dokument_uuid VARCHAR(100) UNIQUE NOT NULL,
    pdf           BYTEA               NOT NULL,
    xml           TEXT                NOT NULL
);

CREATE TABLE KOMMENTAR
(
    kommentar_id     BIGINT      NOT NULL PRIMARY KEY,
    tiltak_id        BIGINT      NOT NULL,
    tekst            TEXT        NOT NULL,
    sist_endret_av   VARCHAR(13) NOT NULL,
    sist_endret_dato TIMESTAMP   NOT NULL,
    opprettet_av     VARCHAR(13) NOT NULL,
    opprettet_dato   TIMESTAMP   NOT NULL,
    CONSTRAINT KOMMENTAR_TILTAK_FK FOREIGN KEY (tiltak_id) REFERENCES TILTAK (tiltak_id)
);

-- Create Indexes
CREATE INDEX oppdialog_aktoerid_index ON OPPFOELGINGSDIALOG (AKTOER_ID);
CREATE INDEX tiltak_oppdialogid_index ON TILTAK (OPPFOELGINGSDIALOG_ID);
CREATE INDEX arboppg_oppdialogid_index ON ARBEIDSOPPGAVE (OPPFOELGINGSDIALOG_ID);
CREATE INDEX godkj_oppdialogid_index ON GODKJENNING (OPPFOELGINGSDIALOG_ID);
CREATE INDEX kommentar_tiltakid_index ON KOMMENTAR (TILTAK_ID);
