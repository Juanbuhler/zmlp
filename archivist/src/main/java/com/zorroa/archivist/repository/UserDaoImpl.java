package com.zorroa.archivist.repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.zorroa.sdk.domain.Room;
import com.zorroa.sdk.domain.User;
import com.zorroa.sdk.domain.UserBuilder;
import com.zorroa.sdk.domain.UserUpdateBuilder;
import com.zorroa.archivist.security.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class UserDaoImpl extends AbstractDao implements UserDao {

    private static final RowMapper<User> MAPPER = (rs, row) -> {
        User user = new User();
        user.setId(rs.getInt("pk_user"));
        user.setUsername(rs.getString("str_username"));
        user.setEmail(rs.getString("str_email"));
        user.setFirstName(rs.getString("str_firstname"));
        user.setLastName(rs.getString("str_lastname"));
        user.setEnabled(rs.getBoolean("bool_enabled"));
        return user;
    };

    @Override
    public User get(int id) {
        return jdbc.queryForObject("SELECT * FROM user WHERE pk_user=?", MAPPER, id);
    }

    @Override
    public User get(String username) {
        return jdbc.queryForObject("SELECT * FROM user WHERE str_username=?", MAPPER, username);
    }

    @Override
    public List<User> getAll() {
        return jdbc.query("SELECT * FROM user ORDER BY str_username", MAPPER);
    }

    private static final String INSERT =
            "INSERT INTO " +
                "user " +
            "(" +
                "str_username, "+
                "str_password, " +
                "str_email, "+
                "str_firstname, " +
                "str_lastname, " +
                "bool_enabled " +
            ") VALUES (?,?,?,?,?,?)";

    @Override
    public User create(UserBuilder builder) {
        Preconditions.checkNotNull(builder.getUsername(), "The Username cannot be null");
        Preconditions.checkNotNull(builder.getPassword(), "The Password cannot be null");
        builder.setPassword(SecurityUtils.createPasswordHash(builder.getPassword()));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_user"});
            ps.setString(1, builder.getUsername());
            ps.setString(2, builder.getPassword());
            ps.setString(3, builder.getEmail());
            ps.setString(4, builder.getFirstName());
            ps.setString(5, builder.getLastName());
            ps.setBoolean(6, true);
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public boolean delete(User user) {
        return jdbc.update("DELETE FROM user WHERE pk_user=?", user.getId()) == 1;
    }

    @Override
    public boolean update(User user, UserUpdateBuilder builder) {

        List<String> updates = Lists.newArrayList();
        List<Object> values = Lists.newArrayList();

        StringBuilder sb = new StringBuilder(512);
        sb.append("UPDATE user SET ");

        if (builder.getUsername() != null) {
            updates.add("str_username=?");
            values.add(builder.getUsername());
        }

        if (builder.getPassword() != null) {
            updates.add("str_password=?");
            values.add(SecurityUtils.createPasswordHash(builder.getPassword()));
        }

        if (builder.getEmail() != null) {
            updates.add("str_email=?");
            values.add(builder.getEmail());
        }

        if (builder.getFirstName() != null) {
            updates.add("str_firstname=?");
            values.add(builder.getFirstName());
        }

        if (builder.getLastName() != null) {
            updates.add("str_lastname=?");
            values.add(builder.getLastName());
        }

        if (builder.getEnabled() != null) {
            updates.add("bool_enabled=?");
            values.add(builder.getEnabled());
        }

        if (values.isEmpty()) {
            return false;
        }

        sb.append(StringUtils.join(updates, ", "));
        sb.append(" WHERE pk_user=?");
        values.add(user.getId());

        logger.debug("updating user '{}', {}", sb.toString(), values);
        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }

    @Override
    public String getPassword(String username) {
        return jdbc.queryForObject("SELECT str_password FROM user WHERE str_username=? AND bool_enabled=?",
            String.class, username, true);
    }

    private static final String GET_ALL_BY_ROOM =
            "SELECT " +
            "user.* " +
            "FROM " +
            "user,session,map_session_to_room m " +
            "WHERE " +
            "session.pk_session = m.pk_session " +
            "AND " +
            "m.pk_room = ? " +
            "AND " +
            "session.pk_user = user.pk_user " +
            "ORDER BY "+
            "user.str_username ASC";

    @Override
    public List<User> getAll(Room room) {
        return jdbc.query(GET_ALL_BY_ROOM, MAPPER, room.getId());
    }

}
