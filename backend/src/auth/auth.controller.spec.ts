import { Test, TestingModule } from '@nestjs/testing';
import { BadRequestException, UnauthorizedException } from '@nestjs/common';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';

describe('AuthController', () => {
  let controller: AuthController;
  let authService: AuthService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [AuthController],
      providers: [
        {
          provide: AuthService,
          useValue: {
            register: jest.fn(),
            login: jest.fn(),
            getProfileByUserId: jest.fn(),
          },
        },
      ],
    }).compile();

    controller = module.get<AuthController>(AuthController);
    authService = module.get<AuthService>(AuthService);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  describe('POST /auth/register', () => {
    it('should call authService.register', async () => {
      const registerDto = {
        firstName: 'John',
        lastName: 'Doe',
        emailOrPhone: 'test@example.com',
        password: 'password123',
        subject: 'Math',
      };

      (authService.register as jest.Mock).mockResolvedValue({
        user: { id: 'user-1' },
        access_token: 'token',
      });

      const result = await controller.register(registerDto);

      expect(result.success).toBe(true);
      expect(authService.register).toHaveBeenCalledWith(registerDto);
    });
  });

  describe('POST /auth/login', () => {
    it('should call authService.login', async () => {
      const loginDto = {
        emailOrPhone: 'test@example.com',
        password: 'password123',
      };

      (authService.login as jest.Mock).mockResolvedValue({
        user: { id: 'user-1' },
        access_token: 'token',
      });

      const result = await controller.login(loginDto);

      expect(result.success).toBe(true);
      expect(authService.login).toHaveBeenCalledWith(loginDto);
    });
  });

  describe('GET /auth/me', () => {
    it('should require JWT authentication', async () => {
      const mockRequest = {
        user: { sub: 'user-123' },
      };

      (authService.getProfileByUserId as jest.Mock).mockResolvedValue({
        user: { id: 'user-123' },
        teacherProfile: { id: 'profile-1' },
      });

      const result = await controller.getProfile(mockRequest);

      expect(result.success).toBe(true);
      expect(authService.getProfileByUserId).toHaveBeenCalledWith('user-123');
    });
  });
});
