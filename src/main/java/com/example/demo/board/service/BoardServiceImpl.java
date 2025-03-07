package com.example.demo.board.service;

import com.example.demo.account.controller.response.MyPageResponse;
import com.example.demo.account.entity.Account;
import com.example.demo.account.repository.AccountRepository;
import com.example.demo.board.controller.form.BoardRequestForm;
import com.example.demo.board.controller.response.BoardListResponse;
import com.example.demo.board.controller.response.BoardReadResponse;
import com.example.demo.board.controller.response.BoardResponse;
import com.example.demo.board.entity.Board;
import com.example.demo.board.repository.BoardRepository;
import com.example.demo.security.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    final private BoardRepository boardRepository;
    final private AccountRepository accountRepository;
    final private JwtProvider jwtProvider;

    // 게시글 리스트
    @Override
    public List<BoardListResponse> list() {
        List<Board> boards = boardRepository.findAll(Sort.by(Sort.Direction.DESC, "boardId"));

        return boards.stream()
                .map(board -> new BoardListResponse(board.getBoardId(), board.getTitle(), board.getWriter(), board.getCreatedData()))
                .collect(Collectors.toList());
    }

    // 게시글 등록
    @Override
    public BoardResponse register(String accessToken, BoardRequestForm form) {
        SecretKey key = jwtProvider.getKey();
        Jws<Claims> claims = Jwts.parser().setSigningKey(key)
                .parseClaimsJws(accessToken.replace(" ", "").replace("Bearer", ""));

        String email = claims.getBody().getSubject();
        email = email.substring(email.indexOf("\"email\":\"") + 9, email.indexOf("\",\"types\""));
        Optional<Account> maybeAccount = accountRepository.findByEmail(email);

        if (maybeAccount.isPresent()) {
            Account account = maybeAccount.get();
            form.setWriter(account.getName());
            Board board = new Board(form.getTitle(), form.getWriter(), form.getContent());
            board.setAccount(account);
            boardRepository.save(board);

            BoardResponse response = new BoardResponse(board.getBoardId(), board.getTitle(), board.getWriter(), board.getContent());
            return response;
        }
        return null;
    }

    @Override
    public BoardReadResponse read(Long boardId) {
        Optional<Board> maybeBoard = boardRepository.findById(boardId);
        if (maybeBoard.isEmpty()) {
            return null;
        }
        Board board = maybeBoard.get();
        BoardReadResponse response = new BoardReadResponse(board.getAccount().getId(), board.getBoardId(), board.getTitle(), board.getWriter(), board.getContent(), board.getCreatedData());
        return response;
    }

    @Override
    public void delete(Long boardId, String accessToken) throws RuntimeException {
        SecretKey key = jwtProvider.getKey();
        Jws<Claims> claims = Jwts.parser().setSigningKey(key)
                .parseClaimsJws(accessToken.replace(" ", "").replace("Bearer", ""));

        String email = claims.getBody().getSubject();
        email = email.substring(email.indexOf("\"email\":\"") + 9, email.indexOf("\",\"types\""));
        Optional<Account> maybeAccount = accountRepository.findByEmail(email);
        Long accountId = maybeAccount.get().getId();

        Optional<Board> maybeBoard = boardRepository.findById(boardId);
        if (maybeBoard.isPresent()) {
            Board board = maybeBoard.get();

            if (board.getAccount() != null && board.getAccount().getId().equals(accountId)) {
                boardRepository.deleteById(boardId);
            } else {
                throw new RuntimeException("삭제 권한이 없습니다.");
            }
        } else {
            throw new RuntimeException("삭제할 게시물을 찾을 수 없습니다.");
        }
    }

    @Override
    public BoardResponse modify(Long boardId, BoardRequestForm form, String accessToken) {
        SecretKey key = jwtProvider.getKey();
        Jws<Claims> claims = Jwts.parser().setSigningKey(key)
                .parseClaimsJws(accessToken.replace(" ", "").replace("Bearer", ""));

        String email = claims.getBody().getSubject();
        email = email.substring(email.indexOf("\"email\":\"") + 9, email.indexOf("\",\"types\""));
        Optional<Account> maybeAccount = accountRepository.findByEmail(email);
        Long accountId = maybeAccount.get().getId();

        Optional<Board> maybeBoard = boardRepository.findById(boardId);
        if (maybeBoard.isPresent()) {
            Board board = maybeBoard.get();

            if (board.getAccount() != null && board.getAccount().getId().equals(accountId)) {

                board.setTitle(form.getTitle());
                board.setContent(form.getContent());
                boardRepository.save(board);

                BoardResponse response = new BoardResponse(board.getBoardId(), board.getTitle(), board.getWriter(), board.getContent());
                return response;
            }
        }
        throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
    }

    @Override
    public List<BoardListResponse> search(String title) {
        if (title == null || title.isEmpty()) {
            return Collections.emptyList();
        }

        List<Board> boards = boardRepository.findByKeyword(title);

        return boards.stream()
                .map(board -> new BoardListResponse(board.getBoardId(), board.getTitle(), board.getWriter(), board.getCreatedData()))
                .collect(Collectors.toList());
    }
}