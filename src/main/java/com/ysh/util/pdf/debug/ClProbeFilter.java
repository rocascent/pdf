package com.ysh.util.pdf.debug;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 临时诊断：打印 Content-Length 由谁设置（用后即删）。 */
@Component
public class ClProbeFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(req, new HttpServletResponseWrapper(res) {
            @Override
            public void setContentLength(int len) {
                if (len > 1000) new Exception("setContentLength(" + len + ") caller:").printStackTrace();
                super.setContentLength(len);
            }
            @Override
            public void setContentLengthLong(long len) {
                if (len > 1000) new Exception("setContentLengthLong(" + len + ") caller:").printStackTrace();
                super.setContentLengthLong(len);
            }
            @Override
            public void setBufferSize(int size) {
                if (size > 100000) new Exception("setBufferSize(" + size + ") caller:").printStackTrace();
                super.setBufferSize(size);
            }
        });
    }
}
