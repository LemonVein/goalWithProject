# GOALWITH PROJECT - Backend Server

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?style=flat-square&logo=redis&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS-S3-232F3E?style=flat-square&logo=amazon-aws&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)

GOALWITH는 사용자가 목표(Quest)를 설정하고, 팀원들과 함께 인증하며 성장하는 **소셜 목표 달성 플랫폼**입니다.

[App Store](https://apps.apple.com/kr/app/goalwith-%EB%AA%A9%ED%91%9C%EB%84%A4%ED%8A%B8%EC%9B%8C%ED%82%B9-%EC%8A%B5%EA%B4%80-%ED%88%AC%EB%91%90-%EB%A3%A8%ED%8B%B4-%EA%B0%93%EC%83%9D/id6756700186)

---

## 목차
1. [프로젝트 개요](#-프로젝트-개요)
2. [기술 스택](#-기술-스택)
3. [시스템 아키텍처](#-시스템-아키텍처)
4. [핵심 기능](#-핵심-기능)
5. [기술적 특이사항](#-기술적-특이사항)

---

## 프로젝트 개요
GOALWITH는 혼자서는 지키기 힘든 목표들을 **퀘스트 기반 인증 시스템**과 **게이미피케이션(캐릭터, 뱃지)** 요소를 통해 지속 가능하게 만드는 웹 서비스입니다. 그리고 이 프로젝트는 해당 어플리케이션의 백엔드 서버 프로젝트입니다.

- **Backend:** RESTful API 설계
- **Infra:** Docker Compose를 활용한 컨테이너 기반 배포 환경

---

## 기술 스택

### Core
- **Language:** Java 17
- **Framework:** Spring Boot 3.4.2

### Database & Cache
- **RDBMS:** MySQL 8.0 (JPA / Hibernate)
- **Query Builder:** QueryDSL 5.0
- **NoSQL:** Redis

### Security & Auth
- **Authentication:** Spring Security + JWT (Access/Refresh Token)
- **OAuth 2.0:** Kakao, Google, Apple Login

### Infrastructure & DevOps
- **Cloud Storage:** AWS S3 (이미지 업로드)
- **Container:** Docker, Docker Compose
- **Testing:** JUnit5, Mockito

---

## 시스템 아키텍처

- **API Server:** 클라이언트(Web/App) 요청 처리
- **DB Layer:** MySQL(영속성 데이터) + Redis(임시 데이터 및 캐싱)
- **File Server:** AWS S3를 통한 이미지 호스팅
- **Admin System:** 별도의 뷰 없이 데이터 API만 제공하는 Headless 구조

---

## 핵심 기능

### 1. 사용자 관리 (User & Auth)
- **JWT 기반 인증:** Stateless한 보안 아키텍처 구현.
- **소셜 로그인:** OAuth2 클라이언트를 커스텀하여 다중 소셜 로그인 지원.
- **하이브리드 토큰 관리:** - Access Token: 클라이언트 저장
  - Refresh Token: DB/Redis 이원화 관리로 보안성과 성능 동시 확보

### 2. 퀘스트 & 인증 (Quest & Verification)
- **퀘스트 생성/참여:** 개인 및 팀 단위 퀘스트(예정) 생성.
- **인증 시스템:** 이미지 업로드(S3)와 함께 인증 기록(Record) 작성.
- **피드백:** 팀원 간 인증 게시물에 대한 댓글 및 리액션 기능.

### 3. 게이미피케이션 (Gamification)
- **캐릭터/뱃지 시스템:** 활동 점수에 따른 캐릭터 성장 및 희귀 뱃지 획득.
- **착용 시스템:** 획득한 아이템 중 '착용(Equipped)' 상태 관리 로직 구현.

### 4. 관리자 시스템 (Admin)
- **Headless CMS:** 프론트엔드 종속성 없는 순수 데이터 API 제공.
- **회원 관리:** 유저 정지(Soft Delete) 및 강제 로그아웃(토큰 폐기) 기능.

---

## 기술적 특이사항

### DTO 변환 레이어 분리 (SRP 준수)
- **문제:** 컨트롤러와 서비스 계층에 엔티티<->DTO 변환 로직이 혼재되어 코드 복잡도 증가.
- **해결:** `DtoConverterService`를 별도로 분리. 엔티티의 연관 관계(Lazy Loading)를 고려한 안전한 변환 로직을 한곳에서 관리하여 유지보수성 향상.

