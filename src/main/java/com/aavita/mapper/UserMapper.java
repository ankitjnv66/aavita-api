package com.aavita.mapper;

import com.aavita.dto.user.UserResponse;
import com.aavita.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
