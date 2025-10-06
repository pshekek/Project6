package rita.mapping;


import org.mapstruct.Mapper;
import rita.dto.UserDto;
import rita.dto.UserRegisterRequest;
import rita.repository.User;

@Mapper(componentModel = "spring")
public interface UserMapping {

    UserDto toDto(User user);

    User toEntityCreate(UserRegisterRequest request);
}
