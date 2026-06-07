package com.goandstudybackend.security;

import com.goandstudybackend.entity.Admin;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.entity.Staff;
import com.goandstudybackend.repository.AdminRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final StaffRepository staffRepository;
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByEmail(username).orElse(null);
        if (admin != null) {
            return new User(admin.getId(), admin.getPasswordHash(), List.of(new SimpleGrantedAuthority(admin.getRole())));
        }

        Staff staff = staffRepository.findByEmail(username).orElse(null);
        if (staff != null) {
            return new User(staff.getId(), staff.getPasswordHash(), List.of(new SimpleGrantedAuthority(staff.getRole())));
        }

        Member member = memberRepository.findByEmail(username).orElse(null);
        if (member != null) {
            return new User(member.getId(), member.getPasswordHash(), List.of(new SimpleGrantedAuthority(member.getRole())));
        }

        throw new UsernameNotFoundException("User not found");
    }
}
