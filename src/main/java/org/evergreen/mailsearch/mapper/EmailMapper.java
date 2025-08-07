package org.evergreen.mailsearch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yourcompany.mailsearch.entity.Email;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailMapper extends BaseMapper<Email> {
}
