import { Test, TestingModule } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import { BadRequestException, UnauthorizedException } from '@nestjs/common';
import { AuthService } from './auth.service';
import { PrismaService } from '../prisma/prisma.service';
import * as bcrypt from 'bcrypt';

jest.mock('bcrypt');

describe('AuthService', () => {
  let service: AuthService;
  let prisma: PrismaService;
  let jwtService: JwtService;

  const mockUser = {
    id: 'user-123',
    emailOrPhone: 'teacher@example.com',
    passwordHash: 'hashed-password',
    salt: 'salt-value',
    createdAt: new Date(),
    updatedAt: new Date(),
  };

  const mockTeacherProfile = {
    id: 'profile-123',
    userId: 'user-123',
    firstName: 'John',
    lastName: 'Doe',
    emailOrPhone: 'teacher@example.com',
    language: "O'zbekcha",
    subject: 'Mathematics',
    school: 'School 1',
    experience: '5 years',
    experienceYears: 5,
    photo: null,
    phone: '',
    email: '',
    createdAt: new Date(),
    updatedAt: new Date(),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        {
          provide: PrismaService,
          useValue: {
            user: {
              findUnique: jest.fn(),
              create: jest.fn(),
            },
            teacherProfile: {
              create: jest.fn(),
            },
            $transaction: jest.fn(),
          },
        },
        {
          provide: JwtService,
          useValue: {
            sign: jest.fn().mockReturnValue('jwt-token'),
          },
        },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
    prisma = module.get<PrismaService>(PrismaService);
    jwtService = module.get<JwtService>(JwtService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('register', () => {
    it('should successfully register a new user', async () => {
      const registerDto = {
        firstName: 'John',
        lastName: 'Doe',
        emailOrPhone: 'newteacher@example.com',
        password: 'password123',
        subject: 'Mathematics',
        school: 'School 1',
        language: "O'zbekcha",
        experience: '5 years',
      };

      (prisma.user.findUnique as jest.Mock).mockResolvedValue(null);
      (bcrypt.genSalt as jest.Mock).mockResolvedValue('salt');
      (bcrypt.hash as jest.Mock).mockResolvedValue('hashed-password');
      (prisma.$transaction as jest.Mock).mockImplementation(
        (callback) =>
          callback({
            user: { create: jest.fn().mockResolvedValue(mockUser) },
            teacherProfile: {
              create: jest.fn().mockResolvedValue(mockTeacherProfile),
            },
          }),
      );

      const result = await service.register(registerDto);

      expect(result).toHaveProperty('access_token');
      expect(result).toHaveProperty('user');
      expect(result).toHaveProperty('teacherProfile');
    });

    it('should throw BadRequestException if user already exists', async () => {
      const registerDto = {
        firstName: 'John',
        lastName: 'Doe',
        emailOrPhone: 'existing@example.com',
        password: 'password123',
        subject: 'Mathematics',
      };

      (prisma.user.findUnique as jest.Mock).mockResolvedValue(mockUser);

      await expect(service.register(registerDto)).rejects.toThrow(
        BadRequestException,
      );
    });

    it('should hash password before storing', async () => {
      const registerDto = {
        firstName: 'John',
        lastName: 'Doe',
        emailOrPhone: 'newteacher@example.com',
        password: 'password123',
        subject: 'Mathematics',
      };

      (prisma.user.findUnique as jest.Mock).mockResolvedValue(null);
      (bcrypt.genSalt as jest.Mock).mockResolvedValue('salt');
      (bcrypt.hash as jest.Mock).mockResolvedValue('hashed-password');
      (prisma.$transaction as jest.Mock).mockImplementation(
        (callback) =>
          callback({
            user: { create: jest.fn().mockResolvedValue(mockUser) },
            teacherProfile: {
              create: jest.fn().mockResolvedValue(mockTeacherProfile),
            },
          }),
      );

      await service.register(registerDto);

      expect(bcrypt.hash).toHaveBeenCalledWith('password123', 'salt');
    });
  });

  describe('login', () => {
    it('should successfully login a user', async () => {
      const loginDto = {
        emailOrPhone: 'teacher@example.com',
        password: 'password123',
      };

      (prisma.user.findUnique as jest.Mock).mockResolvedValue({
        ...mockUser,
        teacherProfile: mockTeacherProfile,
      });
      (bcrypt.compare as jest.Mock).mockResolvedValue(true);

      const result = await service.login(loginDto);

      expect(result).toHaveProperty('access_token');
      expect(result).toHaveProperty('user');
      expect(result).toHaveProperty('teacherProfile');
    });

    it('should throw UnauthorizedException if user not found', async () => {
      const loginDto = {
        emailOrPhone: 'nonexistent@example.com',
        password: 'password123',
      };

      (prisma.user.findUnique as jest.Mock).mockResolvedValue(null);

      await expect(service.login(loginDto)).rejects.toThrow(
        UnauthorizedException,
      );
    });

    it('should throw UnauthorizedException if password is incorrect', async () => {
      const loginDto = {
        emailOrPhone: 'teacher@example.com',
        password: 'wrongpassword',
      };

      (prisma.user.findUnique as jest.Mock).mockResolvedValue({
        ...mockUser,
        teacherProfile: mockTeacherProfile,
      });
      (bcrypt.compare as jest.Mock).mockResolvedValue(false);

      await expect(service.login(loginDto)).rejects.toThrow(
        UnauthorizedException,
      );
    });
  });

  describe('getProfileByUserId', () => {
    it('should return authenticated user profile', async () => {
      (prisma.user.findUnique as jest.Mock).mockResolvedValue({
        ...mockUser,
        teacherProfile: mockTeacherProfile,
      });

      const result = await service.getProfileByUserId('user-123');

      expect(result).toHaveProperty('user');
      expect(result).toHaveProperty('teacherProfile');
      expect(result.user.id).toBe('user-123');
    });

    it('should only return the authenticated user own profile', async () => {
      const userId = 'user-123';
      (prisma.user.findUnique as jest.Mock).mockResolvedValue({
        ...mockUser,
        teacherProfile: mockTeacherProfile,
      });

      const result = await service.getProfileByUserId(userId);

      expect(prisma.user.findUnique).toHaveBeenCalledWith({
        where: { id: userId },
        include: { teacherProfile: true },
      });
      expect(result.user.id).toBe(userId);
    });
  });
});
