package kr.zziririt.zziririt.api.board.service

import kr.zziririt.zziririt.api.board.dto.request.*
import kr.zziririt.zziririt.domain.board.model.BoardCategoryEntity
import kr.zziririt.zziririt.domain.board.model.CategoryEntity
import kr.zziririt.zziririt.domain.board.repository.BoardCategoryRepository
import kr.zziririt.zziririt.domain.board.repository.BoardRepository
import kr.zziririt.zziririt.domain.board.repository.CategoryRepository
import kr.zziririt.zziririt.domain.board.repository.StreamerBoardApplicationRepository
import kr.zziririt.zziririt.domain.member.repository.SocialMemberRepository
import kr.zziririt.zziririt.global.exception.ErrorCode
import kr.zziririt.zziririt.global.exception.ModelNotFoundException
import kr.zziririt.zziririt.global.exception.RestApiException
import kr.zziririt.zziririt.infra.aws.S3Service
import kr.zziririt.zziririt.infra.querydsl.board.BoardRowDto
import kr.zziririt.zziririt.infra.querydsl.board.StreamerBoardRowDto
import kr.zziririt.zziririt.infra.security.UserPrincipal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile


@Service
class BoardService(
    private val socialMemberRepository: SocialMemberRepository,
    private val boardRepository: BoardRepository,
    private val streamerBoardApplicationRepository: StreamerBoardApplicationRepository,
    private val s3Service: S3Service,
    private val categoryRepository: CategoryRepository,
    private val boardCategoryRepository: BoardCategoryRepository
) {
    @Transactional
    fun createStreamerBoardApplication(
        multipartFile: List<MultipartFile>,
        streamerBoardApplicationRequest: StreamerBoardApplicationRequest,
        userPrincipal: UserPrincipal
    ) {
        val findSocialMember = socialMemberRepository.findByIdOrNull(userPrincipal.memberId)
            ?: throw ModelNotFoundException(ErrorCode.MODEL_NOT_FOUND)

        check(!boardRepository.existsBoardEntityByBoardName(streamerBoardApplicationRequest.applyBoardName)) {
            throw RestApiException(ErrorCode.DUPLICATE_MODEL_NAME)
        }

        val saveForm =
            streamerBoardApplicationRepository.save(streamerBoardApplicationRequest.to(socialMemberEntity = findSocialMember))
        val imageUrl = s3Service.uploadFiles(dir = "streamer_image", files = multipartFile)
        saveForm.uploadImage(imageUrl.toString())

    }

    @Transactional
    fun updateStreamerBoardApplication(
        streamerBoardApplicationRequest: StreamerBoardApplicationRequest,
        multipartFile: List<MultipartFile>,
        userPrincipal: UserPrincipal
    ) {
        socialMemberRepository.findByIdOrNull(userPrincipal.memberId)
            ?: throw ModelNotFoundException(ErrorCode.MODEL_NOT_FOUND)

        val findStreamerForm = streamerBoardApplicationRepository.findByIdOrNull(userPrincipal.memberId)
            ?: throw ModelNotFoundException(ErrorCode.MODEL_NOT_FOUND)
        findStreamerForm.update(
            applyUrl = streamerBoardApplicationRequest.applyUrl,
            applyBoardName = streamerBoardApplicationRequest.applyBoardName
        )

        val imageUrl = s3Service.uploadFiles(dir = "update_streamer_image", files = multipartFile)
        findStreamerForm.uploadImage(imageUrl.toString())
    }

    @Transactional
    fun createBoard(boardRequest: BoardRequest, userPrincipal: UserPrincipal) {
        val findMember = socialMemberRepository.findByIdOrNull(userPrincipal.memberId)
            ?: throw RestApiException(ErrorCode.MODEL_NOT_FOUND)

        boardRepository.save(boardRequest.to(socialMemberEntity = findMember))
    }

    @Transactional
    fun updateBoard(boardId: Long, boardRequest: BoardRequest, userPrincipal: UserPrincipal) {
        val findMember = socialMemberRepository.findByIdOrNull(userPrincipal.memberId) ?: throw RestApiException(
            ErrorCode.MODEL_NOT_FOUND
        )

        val findBoard =
            boardRepository.findByIdOrNull(boardId) ?: throw ModelNotFoundException(ErrorCode.MODEL_NOT_FOUND)

        findBoard.update(boardName = boardRequest.boardName, socialMember = findMember)
    }

    fun deleteBoard(boardId: Long, userPrincipal: UserPrincipal) {
        val findBoard =
            boardRepository.findByIdOrNull(boardId) ?: throw ModelNotFoundException(ErrorCode.MODEL_NOT_FOUND)

        boardRepository.delete(findBoard)
    }

    @Transactional
    fun createSubscribeBoard(subscribeBoardRequest: SubscribeBoardRequest, userPrincipal: UserPrincipal) {
        val findSocialMember = socialMemberRepository.findByIdOrNull(userPrincipal.memberId)
            ?: throw ModelNotFoundException(ErrorCode.MODEL_NOT_FOUND)
        val findBoard =
            boardRepository.findByIdOrNull(subscribeBoardRequest.subscribeBoardId) ?: throw ModelNotFoundException(
                ErrorCode.MODEL_NOT_FOUND
            )

        findSocialMember.subscribeBoardsList.add(findBoard.id!!)

        socialMemberRepository.save(findSocialMember)
    }

    @Transactional
    fun unSubscribeBoard(subscribeBoardRequest: SubscribeBoardRequest, userPrincipal: UserPrincipal) {
        val findSocialMember = socialMemberRepository.findByIdOrNull(userPrincipal.memberId)
            ?: throw ModelNotFoundException(ErrorCode.MODEL_NOT_FOUND)
        val findBoard =
            boardRepository.findByIdOrNull(subscribeBoardRequest.subscribeBoardId) ?: throw ModelNotFoundException(
                ErrorCode.MODEL_NOT_FOUND
            )

        findSocialMember.subscribeBoardsList.remove(findBoard.id)

        socialMemberRepository.save(findSocialMember)
    }

    fun getBoards(pageable: Pageable): Page<BoardRowDto> {
        return boardRepository.findByPageable(pageable)
    }

    fun getActiveStatusBoards(pageable: Pageable): Page<BoardRowDto> {
        return boardRepository.findActiveStatusBoards(pageable)
    }

    fun getStreamers(): List<StreamerBoardRowDto> {
        return boardRepository.findStreamers()
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    fun boardScheduler() {
        val inactiveBoardIdList = boardRepository.findInactiveBoardStatus()

        boardRepository.updateBoardStatusToInactive(inactiveBoardIdList)
    }

    @Transactional
    fun createStreamerBoard(streamerBoardRequest: StreamerBoardRequest, userPrincipal: UserPrincipal) {
        val boardOwner = socialMemberRepository.findByIdOrNull(streamerBoardRequest.boardOwnerId)
            ?: throw RestApiException(ErrorCode.MODEL_NOT_FOUND)

        val board = boardRepository.save(streamerBoardRequest.to(boardOwner))

        val categoryNames = listOf("공지 사항", "잡담 게시판")
        val categories = categoryNames.map { CategoryEntity(it) }
        categories.forEach {
            categoryRepository.save(it)
            boardCategoryRepository.save(BoardCategoryEntity(board, it))
        }
    }

    @Transactional
    fun addCategoryToBoard(boardId: Long, request: CreateCategoryRequest) {

        val boardCheck = boardRepository.findByIdOrNull(boardId)
            ?: throw ModelNotFoundException(ErrorCode.MODEL_NOT_FOUND)

        val category = categoryRepository.save(request.toEntity())

        val categoryCheck = categoryRepository.findByIdOrNull(category.id)
            ?: throw ModelNotFoundException(ErrorCode.MODEL_NOT_FOUND)

        val boardCategory = BoardCategoryEntity(boardCheck, categoryCheck)

        boardCategoryRepository.save(boardCategory)
    }

    fun getCategoriesByBoardId(boardId: Long): List<BoardCategoryResponse> {
        val board = boardRepository.findByIdOrNull(boardId)
            ?: throw ModelNotFoundException(ErrorCode.MODEL_NOT_FOUND)

        val boardCategories = boardCategoryRepository.findByBoardId(board.id!!)

        return boardCategories.map { BoardCategoryResponse.from(it) }
    }

}