package com.softropic.promora.security.core.mapper;



import com.softropic.promora.security.exposed.UserDto;
import com.softropic.promora.security.domain.Authority;
import com.softropic.promora.security.domain.User;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Persistence;


@Mapper(componentModel = "spring", imports = {org.apache.commons.lang3.StringUtils.class})
public interface UserMapper {
    @Mapping(source = "dateOfBirth", target = "dob")
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "login", expression = "java(StringUtils.lowerCase(user.getLogin()))")
    @Mapping(target = "email", expression = "java(StringUtils.lowerCase(user.getEmail()))")
    UserDto toUserDto(User user);

    @Mapping(source = "dob", target = "dateOfBirth")
    User toUser(UserDto userDto);

    default Set<String> toAuthStrings(Set<Authority> auths) {
        return auths.stream().map(Authority::getName).collect(Collectors.toSet());
    }

    @AfterMapping
    default void mapAuthToString(User user, @MappingTarget UserDto userDto) {
        if(Persistence.getPersistenceUtil().isLoaded(user, "authorities")) {
            final Set<String> auths = user.getAuthorities()
                                            .stream()
                                            .map(Authority::getName)
                                            .collect(Collectors.toSet());
            userDto.setAuthorities(auths);
        }
    }

    /**
     * The method default Set<String> toAuthStrings(Set<Authority> auths) is not added so as to avoid lazyInitializationException
     * @param auths
     * @return
     */
    default Set<Authority> toAuth(Set<String> auths) {
        return auths.stream().map(Authority::new).collect(Collectors.toSet());
    }
}
