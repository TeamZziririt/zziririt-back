package kr.zziririt.zziririt.api.board.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.Valid
import kr.zziririt.zziririt.api.board.dto.request.*
import kr.zziririt.zziririt.api.board.service.BoardService
import kr.zziririt.zziririt.api.board.service.CategoryService
import kr.zziririt.zziririt.api.dto.CommonResponse
import kr.zziririt.zziririt.global.responseEntity
import kr.zziririt.zziririt.infra.security.UserPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/boards")
class BoardController(
    private val boardService: BoardService,
    private val categoryService: CategoryService
) {
    @PostMapping("/apply")
    fun createStreamerApply(
        @RequestParam("image") multipartFile: List<MultipartFile>,
        @RequestParam request: String,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CommonResponse<Nothing>> {
        val mapper = jacksonObjectMapper()
        val streamerBoardApplicationRequest = mapper.readValue(request, StreamerBoardApplicationRequest::class.java)
        boardService.createStreamerBoardApplication(multipartFile, streamerBoardApplicationRequest, userPrincipal)
        return responseEntity(HttpStatus.OK)
    }

    @PutMapping("/apply")
    fun updateStreamerApply(
        @RequestPart("image") multipartFile: List<MultipartFile>,
        @Valid @RequestPart streamerBoardApplicationRequest: StreamerBoardApplicationRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CommonResponse<Nothing>> {
        boardService.updateStreamerBoardApplication(streamerBoardApplicationRequest, multipartFile, userPrincipal)
        return responseEntity(HttpStatus.OK)
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createBoard(
        @Valid @RequestBody boardRequest: BoardRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CommonResponse<Nothing>> {
        boardService.createBoard(boardRequest, userPrincipal)
        return responseEntity(HttpStatus.CREATED)
    }

    @PostMapping("/streamer")
    @PreAuthorize("hasRole('ADMIN')")
    fun createStreamerBoard(
        @Valid @RequestBody streamerBoardRequest: StreamerBoardRequest
    ): ResponseEntity<CommonResponse<Nothing>> {
        boardService.createStreamerBoard(streamerBoardRequest)
        return responseEntity(HttpStatus.CREATED)
    }

    @PutMapping("/{boardId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateBoard(
        @PathVariable boardId: Long,
        @Valid @RequestBody boardRequest: BoardRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CommonResponse<Nothing>> {
        boardService.updateBoard(boardId, boardRequest, userPrincipal)
        return responseEntity(HttpStatus.OK)
    }

    @DeleteMapping("/{boardId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteBoard(
        @PathVariable boardId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CommonResponse<Nothing>> {
        boardService.deleteBoard(boardId, userPrincipal)
        return responseEntity(HttpStatus.NO_CONTENT)
    }

    @PostMapping("/subscribe")
    fun createSubscribeBoard(
        @RequestBody subscribeBoardRequest: SubscribeBoardRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CommonResponse<Nothing>> {
        boardService.createSubscribeBoard(subscribeBoardRequest, userPrincipal)
        return responseEntity(HttpStatus.CREATED)
    }

    @PostMapping("/unsubscribe")
    fun unSubscribeBoard(
        @RequestBody subscribeBoardRequest: SubscribeBoardRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CommonResponse<Nothing>> {
        boardService.unSubscribeBoard(subscribeBoardRequest, userPrincipal)
        return responseEntity(HttpStatus.CREATED)
    }

    @GetMapping
    fun getBoards(
        @PageableDefault(size = 60) pageable: Pageable
    ) = responseEntity(HttpStatus.OK) { boardService.getBoards(pageable) }


    @GetMapping("/active")
    fun getActiveStatusBoards(
        @PageableDefault(size = 60) pageable: Pageable
    ) = responseEntity(HttpStatus.OK) { boardService.getActiveStatusBoards(pageable) }

    @GetMapping("/streamer")
    fun getStreamers() = responseEntity(HttpStatus.OK) { boardService.getStreamers() }

    @GetMapping("/categories/{categoryId}")
    fun getCategoryById(
        @PathVariable categoryId: Long
    ) = responseEntity(HttpStatus.OK) { categoryService.getCategoryById(categoryId) }

    @GetMapping("/categories")
    fun getAllCategories() = responseEntity(HttpStatus.OK) { categoryService.getAllCategories() }

    @PostMapping("/{boardId}")
    fun addCategoryToBoard(
        @PathVariable boardId:Long,
        @RequestBody request: CreateCategoryRequest
    ) = responseEntity(HttpStatus.OK) { boardService.addCategoryToBoard(boardId, request) }

    @GetMapping(("/{boardId}"))
    fun getCategoriesByBoardId(
        @PathVariable boardId: Long
    ) = responseEntity(HttpStatus.OK) { boardService.getCategoriesByBoardId(boardId) }

    @PutMapping("/categories/{categoryId}")
    fun updateCategoryName(
        @PathVariable categoryId: Long,
        @RequestBody request: UpdateCategoryNameRequest
    ) = responseEntity(HttpStatus.OK) { categoryService.updateCategoryById(categoryId, request)}
}