import {
  Injectable,
  BadRequestException,
  UnauthorizedException,
  NotFoundException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import { v4 as uuidv4 } from 'uuid';
import { PrismaService } from '../prisma/prisma.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwtService: JwtService,
  ) {}

  /**
   * Register a new teacher account.
   * Creates User and TeacherProfile in a transaction.
   */
  async register(registerDto: RegisterDto) {
    const { emailOrPhone, password, firstName, lastName, subject, school, language, experience } = registerDto;

    // Check if user already exists
    const existingUser = await this.prisma.user.findUnique({
      where: { emailOrPhone },
    });

    if (existingUser) {
      throw new BadRequestException('Email or phone already registered');
    }

    // Hash password
    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(password, salt);

    try {
      // Create user and teacher profile in a transaction
      const result = await this.prisma.$transaction(async (tx) => {
        // Create user
        const user = await tx.user.create({
          data: {
            id: uuidv4(),
            emailOrPhone,
            passwordHash,
            salt,
          },
        });

        // Create teacher profile
        const teacherProfile = await tx.teacherProfile.create({
          data: {
            id: uuidv4(),
            userId: user.id,
            firstName,
            lastName,
            emailOrPhone,
            language: language || "O'zbekcha",
            subject,
            school: school || '',
            experience: experience || '',
          },
        });

        return {
          user,
          teacherProfile,
        };
      });

      // Generate JWT
      const access_token = this.jwtService.sign({ sub: result.user.id });

      return {
        user: {
          id: result.user.id,
          emailOrPhone: result.user.emailOrPhone,
        },
        teacherProfile: {
          id: result.teacherProfile.id,
          userId: result.teacherProfile.userId,
          firstName: result.teacherProfile.firstName,
          lastName: result.teacherProfile.lastName,
          subject: result.teacherProfile.subject,
        },
        access_token,
      };
    } catch (error) {
      console.error('Registration transaction error:', error);
      throw new BadRequestException('Registration failed');
    }
  }

  /**
   * Login with email/phone and password.
   */
  async login(loginDto: LoginDto) {
    const { emailOrPhone, password } = loginDto;

    // Find user by email or phone
    const user = await this.prisma.user.findUnique({
      where: { emailOrPhone },
      include: {
        teacherProfile: true,
      },
    });

    if (!user) {
      throw new UnauthorizedException('Invalid email/phone or password');
    }

    // Verify password
    const isPasswordValid = await bcrypt.compare(password, user.passwordHash);

    if (!isPasswordValid) {
      throw new UnauthorizedException('Invalid email/phone or password');
    }

    // Generate JWT
    const access_token = this.jwtService.sign({ sub: user.id });

    return {
      user: {
        id: user.id,
        emailOrPhone: user.emailOrPhone,
      },
      teacherProfile: user.teacherProfile
        ? {
            id: user.teacherProfile.id,
            userId: user.teacherProfile.userId,
            firstName: user.teacherProfile.firstName,
            lastName: user.teacherProfile.lastName,
            subject: user.teacherProfile.subject,
          }
        : null,
      access_token,
    };
  }

  /**
   * Get the authenticated user's own profile.
   * Only accessible if JWT is valid.
   */
  async getProfileByUserId(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      include: {
        teacherProfile: true,
      },
    });

    if (!user) {
      throw new NotFoundException('User not found');
    }

    return {
      user: {
        id: user.id,
        emailOrPhone: user.emailOrPhone,
      },
      teacherProfile: user.teacherProfile,
    };
  }

  /**
   * Validate JWT and return the user.
   * Used by JWT strategy.
   */
  async validateJwtPayload(payload: { sub: string }) {
    const user = await this.prisma.user.findUnique({
      where: { id: payload.sub },
      include: {
        teacherProfile: true,
      },
    });

    if (!user) {
      throw new UnauthorizedException('User not found');
    }

    return {
      id: user.id,
      emailOrPhone: user.emailOrPhone,
      teacherProfile: user.teacherProfile,
    };
  }
}
