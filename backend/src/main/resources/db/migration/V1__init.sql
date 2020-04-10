create table courses_watchlist (
    id serial not null,
    section_id int not null,
    term_id int not null,
    email varchar(64) not null,
    last_state_has_space boolean not null,
    friendly_name text not null,
    primary key (section_id, term_id, email)
);